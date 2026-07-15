# camada de persistência

este documento detalha a camada de persistência do blog na segunda entrega. a primeira entrega já usava jpa e repositórios spring data para o crud básico; aqui o foco foi amadurecer essa camada em quatro frentes: modelar os dados olhando para os caminhos de consulta reais, apertar a integridade e a performance com as ferramentas do próprio jpa, adicionar histórico de mudanças com hibernate envers, e cobrir tudo isso com testes automatizados. a arquitetura geral em camadas e bounded contexts continua a mesma e está descrita em [ARQUITETURA.md](ARQUITETURA.md); o que segue é o recorte da persistência.

## modelo de dados

o domínio tem três entidades, distribuídas em dois contextos. no contexto de authoring ficam `Author` e `Post`; no de engagement fica `Comment`. cada uma é um aggregate com o seu próprio ciclo de vida, e essa é a decisão de modelagem mais importante da camada: um post referencia o autor por `authorId` (um `Long`), e não por um objeto `Author` embutido com `@ManyToOne`. o comentário faz o mesmo com o post, guardando só `postId`. não há relacionamento jpa nem chave estrangeira ligando as tabelas.

essa escolha custa uma busca extra quando a api precisa mostrar o nome do autor junto do post (o `PostService` resolve o `authorId` no repositório de autores e monta o `PostResponse`), mas em troca mantém os contextos desacoplados. um aggregate não arrasta o outro numa consulta, cada um pode evoluir sozinho, e se um dia esses contextos virarem serviços separados a referência por id já é o formato natural: vira um identificador que cruza a fronteira de rede, sem uma relação de banco para desmontar. modelar assim é justamente considerar o isolamento de domínio, que era um dos pontos pedidos.

do lado das colunas, cada campo declara explicitamente o que precisa. `Author.email` é `nullable = false, unique = true`, o que vira uma restrição de unicidade no banco e barra dois autores com o mesmo email já na camada de persistência, não só na regra de negócio do service. os textos longos (`Post.content` e `Comment.content`) são `@Lob`. os instantes de criação e publicação são colunas `nullable` conforme a semântica: `createdAt` é obrigatório, `publishedAt` só existe depois que o post é publicado.

### índices alinhados às consultas

os índices não foram colocados por reflexo; cada um cobre uma consulta que a aplicação de fato faz. a tabela `posts` tem três índices declarados no `@Table`:

- `idx_posts_author_id` em `author_id`, porque é por esse campo que se resolve o autor de um post e que se listariam os posts de um autor;
- `idx_posts_status` em `status`, pensando na separação entre rascunho e publicado;
- `idx_posts_created_at` em `created_at`, que é a coluna da ordenação em `findAllByOrderByCreatedAtDesc`, a consulta da home.

a tabela `comments` tem `idx_comments_post_id` em `post_id`, que é o filtro de `findByPostIdOrderByCreatedAtAsc`, a listagem de comentários de um post. em `authors`, a restrição de unicidade do email já cria o índice correspondente, então não precisei declarar outro. em um banco pequeno e em memória como o h2 desta entrega a diferença de tempo é imperceptível, mas os índices documentam a intenção e são o que evita varredura de tabela quando o volume cresce.

### travamento otimista

as três entidades ganharam um campo `@Version private Long version`. o hibernate incrementa esse número a cada atualização e, na hora de gravar, confere se a versão em memória bate com a do banco. se dois fluxos carregarem o mesmo post e tentarem salvar por cima um do outro, o segundo falha com uma `OptimisticLockException` em vez de sobrescrever silenciosamente a alteração do primeiro. é uma proteção de integridade barata, que não segura linha nenhuma no banco (ao contrário do travamento pessimista) e ainda serve de sinal de que a entidade mudou. os testes de repositório verificam esse incremento acontecendo a cada `save`.

## mapeamento objeto-relacional

o mapeamento é todo por anotação jpa, sem xml. `@Entity` e `@Table` definem a tabela; `@Id` com `@GeneratedValue(strategy = IDENTITY)` delega a geração da chave ao banco; `@Column` ajusta nulidade, unicidade e nome; `@Enumerated(EnumType.STRING)` grava o `PostStatus` como texto (`DRAFT`/`PUBLISHED`) em vez de ordinal, para o banco continuar legível e resistente a reordenação do enum; `@Lob` marca os textos longos; `@Version` liga o travamento otimista.

as entidades não expõem setters. o construtor recebe o que é obrigatório e os métodos de intenção (`Author.updateProfile`, `Post.edit`, `Post.publish`) é que mudam o estado, mantendo as invariantes dentro do domínio — `publish`, por exemplo, recusa republicar um post que já está publicado. o construtor protegido sem argumentos existe só porque o jpa exige, e o lombok gera getters e esse construtor para não poluir a classe.

## repositórios spring data

cada aggregate tem um repositório que é uma interface estendendo `JpaRepository`; o spring data gera a implementação em tempo de execução. além do crud herdado, cada um declara as consultas de que a aplicação precisa, escritas como query methods derivados — o próprio nome do método vira a consulta, sem sql na mão:

```java
public interface AuthorRepository extends JpaRepository<Author, Long>, RevisionRepository<Author, Long, Integer> {
    boolean existsByEmail(String email);
}

public interface PostRepository extends JpaRepository<Post, Long>, RevisionRepository<Post, Long, Integer> {
    List<Post> findAllByOrderByCreatedAtDesc();
}

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
```

`existsByEmail` roda um `select` de existência (mais barato que carregar o autor só para checar) e sustenta a regra de email único no `AuthorService`. `findAllByOrderByCreatedAtDesc` traz os posts com os mais recentes no topo, apoiada no índice de `created_at`. `findByPostIdOrderByCreatedAtAsc` filtra os comentários de um post na ordem cronológica, apoiada no índice de `post_id`. alguns métodos herdados também são usados diretamente: `existsById`, no `PostCatalogAdapter`, é o que o contexto de engagement chama para confirmar que um post existe antes de aceitar um comentário, sem enxergar a entidade `Post`.

### exemplos de uso

na prática, os services conversam com esses repositórios assim:

```java
// AuthorService: a consulta derivada sustenta a regra de unicidade
if (authorRepository.existsByEmail(request.email())) {
    throw new BusinessRuleException("Ja existe um autor com o email " + request.email());
}
Author salvo = authorRepository.save(new Author(request.name(), request.email(), request.bio()));

// PostService: a listagem já sai ordenada do banco
List<PostResponse> posts = postRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toResponse)
        .toList();

// atualização por dirty checking: dentro da transação, alterar a entidade
// gerenciada basta; o hibernate detecta a mudança e faz o update no flush
Post post = postRepository.findById(id).orElseThrow(...);
post.edit(request.title(), request.content());
```

o último trecho mostra um detalhe do gerenciamento de dados: o `update` do service não chama `save`. como o método é `@Transactional` e o `post` foi carregado dentro dessa transação, ele é uma entidade gerenciada; alterar seus campos é suficiente para o hibernate emitir o `update` no fim da transação. o `save` explícito fica para a criação, quando a entidade ainda é nova.

## histórico de dados com hibernate envers

o requisito central desta entrega é registrar e consultar o histórico de mudanças dos dados. em vez de escrever tabelas de auditoria e triggers na mão, usei o hibernate envers, que é a extensão do próprio hibernate para isso, integrada ao jpa. a ativação é declarativa: basta anotar a entidade com `@Audited`. as três entidades estão auditadas, então todo o domínio tem trilha.

### como o registro acontece

para cada entidade auditada o envers cria, no schema, uma tabela espelho com o sufixo `_AUD` (`posts_AUD`, `authors_AUD`, `comments_AUD`) e uma tabela global `REVINFO` que numera as revisões e guarda o instante de cada uma. a cada transação que insere, altera ou remove uma entidade auditada, o envers grava uma linha na tabela `_AUD` correspondente com o estado daquele momento e o tipo da operação (`INSERT`, `UPDATE` ou `DELETE`), amarrada ao número da revisão. isso é automático: o service não sabe que existe histórico, ele só faz o crud normal, e o registro é um efeito da transação fechar.

um ponto que precisei configurar de propósito está no `application.yml`:

```yaml
spring:
  jpa:
    properties:
      org.hibernate.envers.store_data_at_delete: true
```

o nome da propriedade tem que trazer o prefixo completo `org.hibernate.envers.`, que é como o envers lê suas configurações — foi exatamente aí que tropecei uma vez, escrevendo `hibernate.envers.` por engano, e a revisão de exclusão saía com os campos nulos. com `store_data_at_delete` ligado, a revisão de `DELETE` guarda o estado que o registro tinha antes de sumir, e não apenas o id. assim o histórico mostra como o post estava no momento em que foi apagado, que é o que dá valor à auditoria.

### como a consulta acontece

do lado da leitura, aproveitei a integração spring data envers: os repositórios de post e de autor estendem também `RevisionRepository<T, ID, Integer>` (o `Integer` é o tipo do número de revisão que o envers gera). isso adiciona à interface métodos como `findRevisions(id)`, que devolve as revisões da entidade em ordem crescente, sem eu escrever nenhuma consulta. para essa fábrica de repositórios entrar em ação foi preciso trocar o factory bean padrão, o que fica em uma classe de configuração:

```java
@Configuration
@EnableJpaRepositories(
        basePackages = "com.blog",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
public class PersistenceConfig { }
```

essa fábrica é uma extensão da fábrica normal de repositórios jpa, então os repositórios que não pedem histórico (como o de comentário, que continua só `JpaRepository`) seguem funcionando igual. o serviço de histórico então lê as revisões e as converte no dto de resposta:

```java
public List<PostRevisionResponse> historyOf(Long postId) {
    List<PostRevisionResponse> history = postRepository.findRevisions(postId).getContent().stream()
            .map(PostRevisionResponse::from)
            .toList();
    if (history.isEmpty()) {
        throw new ResourceNotFoundException("Post " + postId + " nao encontrado");
    }
    return history;
}
```

cada `PostRevisionResponse` junta o estado do post naquela revisão com os metadados que o envers registra: o número da revisão, o tipo (`INSERT`/`UPDATE`/`DELETE`) e o instante. vale notar que um post já apagado continua tendo histórico — `findRevisions` devolve as revisões inclusive a de exclusão —, o que é o comportamento desejado numa trilha de auditoria. a consulta é exposta na api em `GET /api/posts/{id}/history` e `GET /api/authors/{id}/history`, e o front mostra a linha do tempo na página do post.

## gerenciamento de dados: integridade e performance

reunindo o que sustenta a integridade: as restrições de coluna (`nullable`, `unique`) barram dados inválidos no banco; o travamento otimista com `@Version` evita atualizações concorrentes se sobrescreverem; as transações declarativas com `@Transactional` nos services garantem que cada caso de uso é atômico, e as leituras usam `@Transactional(readOnly = true)`, que permite ao hibernate pular verificações de dirty checking desnecessárias. as regras que não são de banco (email único, não republicar) vivem no service e no domínio, e as violações viram respostas http consistentes pelo `GlobalExceptionHandler`.

do lado da performance, além dos índices já descritos, as consultas foram escolhidas para pedir ao banco só o necessário: `existsByEmail` e `existsById` são checagens de existência em vez de carregamentos completos, e as ordenações acontecem no banco (via `order by` do query method) e não em memória. nada disso é otimização prematura pesada; é a higiene esperada de uma camada de persistência que quer escalar sem retrabalho.

## testes da camada de persistência

os testes ficam em `backend/src/test` e cobrem a persistência em duas alturas, com uma decisão técnica no meio que vale explicar.

os testes de repositório (`AuthorRepositoryTest`, `PostRepositoryTest`, `CommentRepositoryTest`) usam `@DataJpaTest`, que sobe apenas a fatia de jpa sobre o h2 e reverte a transação ao fim de cada teste, deixando o banco limpo. eles verificam as consultas derivadas, a restrição de unicidade do email (esperando uma `DataIntegrityViolationException` no flush do duplicado), os valores padrão ao criar um post, a transição de publicação e o incremento da `@Version`. como os repositórios de post e de autor estendem `RevisionRepository`, esses testes importam a `PersistenceConfig` para ter a fábrica do envers disponível.

os testes de histórico (`PostHistoryServiceTest`, `AuthorHistoryServiceTest`, `PostHistoryApiTest`) usam `@SpringBootTest` e, de propósito, não são transacionais. essa é a sutileza: o envers só grava a revisão quando a transação faz commit, então um teste que rodasse tudo dentro de uma transação revertida (como o `@DataJpaTest` faz) não veria histórico nenhum. por isso esses testes chamam os services reais — cada chamada abre e fecha a sua própria transação, que commita e dispara o registro do envers — e limpam o banco no `@AfterEach`. o teste de ciclo de vida cria, edita, publica e apaga um post, e então confere que o histórico tem exatamente quatro revisões, na ordem `INSERT`, `UPDATE`, `UPDATE`, `DELETE`, com os valores certos em cada uma, inclusive o estado preservado na exclusão. o teste de api sobe o contexto web e exercita o endpoint de ponta a ponta, do http até a tabela de auditoria, confirmando o formato da resposta e o 404 de um post inexistente.

para os testes partirem de um banco previsível, o `DataSeeder` (que popula dados de exemplo no startup) está anotado com `@Profile("!test")` e não roda sob o perfil de teste. tudo isso é executado com `./mvnw test` a partir de `backend`.
