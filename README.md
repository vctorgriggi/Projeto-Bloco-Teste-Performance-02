# blog (kissaten)

monólito de blog em Spring Boot com front-end em React. autores escrevem posts, posts viram de rascunho para publicado, e leitores comentam. o projeto começou como uma base organizada em camadas e bounded contexts (primeira entrega) e evoluiu para uma camada de persistência mais completa, com histórico de mudanças dos dados e testes automatizados (segunda entrega).

a explicação completa da arquitetura, com os diagramas de componentes e de sequência, está em [docs/ARQUITETURA.md](docs/ARQUITETURA.md); os detalhes da camada de persistência e do histórico estão em [docs/PERSISTENCIA.md](docs/PERSISTENCIA.md).

## stack

- Java 21 e Spring Boot 3.3 (web, data jpa, validation)
- H2 em memória (zera a cada restart, sem precisar instalar banco)
- Hibernate Envers e Spring Data Envers para o histórico de dados
- React 18 com Vite
- Maven (via wrapper, não precisa instalar)

## estrutura

```
.
├── backend     aplicação Spring Boot (a API)
├── frontend    aplicação React (a interface)
└── docs        documentação de arquitetura
```

## como rodar

precisa de um JDK 21 e do Node 18+ instalados. o Maven vem junto pelo wrapper.

### back-end

```bash
cd backend
./mvnw spring-boot:run
```

a API sobe em `http://localhost:8080`. na primeira execução ela já popula o H2 com alguns autores, posts e comentários de exemplo. o console do banco fica em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:blogdb`, usuário `sa`, sem senha).

### front-end

em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

a interface abre em `http://localhost:5173` e já conversa com a API. suba o back-end antes, senão as telas aparecem com erro de conexão.

## api

tudo fica sob o prefixo `/api`.

### autores

| método | rota                        | o que faz                      |
| ------ | --------------------------- | ------------------------------ |
| GET    | `/api/authors`              | lista os autores               |
| GET    | `/api/authors/{id}`         | busca um autor                 |
| GET    | `/api/authors/{id}/history` | histórico de mudanças do autor |
| POST   | `/api/authors`              | cadastra um autor              |
| PUT    | `/api/authors/{id}`         | atualiza nome e bio            |
| DELETE | `/api/authors/{id}`         | remove um autor                |

### posts

| método | rota                      | o que faz                               |
| ------ | ------------------------- | --------------------------------------- |
| GET    | `/api/posts`              | lista os posts (mais recentes primeiro) |
| GET    | `/api/posts/{id}`         | busca um post                           |
| GET    | `/api/posts/{id}/history` | histórico de revisões do post           |
| POST   | `/api/posts`              | cria um post (nasce como rascunho)      |
| PUT    | `/api/posts/{id}`         | edita título e texto                    |
| POST   | `/api/posts/{id}/publish` | publica um rascunho                     |
| DELETE | `/api/posts/{id}`         | remove um post                          |

### comentários

| método | rota                           | o que faz                       |
| ------ | ------------------------------ | ------------------------------- |
| GET    | `/api/posts/{postId}/comments` | lista os comentários de um post |
| POST   | `/api/posts/{postId}/comments` | adiciona um comentário          |
| DELETE | `/api/comments/{commentId}`    | remove um comentário            |

### exemplo

```bash
# criar um autor
curl -X POST http://localhost:8080/api/authors \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana Souza","email":"ana@blog.dev","bio":"escreve sobre arquitetura"}'

# criar um post para esse autor (supondo id 1)
curl -X POST http://localhost:8080/api/posts \
  -H 'Content-Type: application/json' \
  -d '{"title":"Olá mundo","content":"primeiro post","authorId":1}'

# publicar o post
curl -X POST http://localhost:8080/api/posts/1/publish

# ver o histórico de revisões desse post
curl http://localhost:8080/api/posts/1/history
```

## histórico de dados

toda mudança em um autor, post ou comentário é registrada automaticamente pelo Hibernate Envers em tabelas de auditoria (`posts_AUD`, `authors_AUD`, `comments_AUD`, além da `REVINFO` que numera as revisões). não é preciso fazer nada no fluxo de escrita: criar, editar, publicar ou apagar já grava uma revisão com o estado daquele momento.

a consulta sai pelos endpoints de histórico. cada entrada traz os metadados da revisão (número, tipo — `INSERT`, `UPDATE` ou `DELETE` — e instante) junto do estado do registro naquele ponto. um post apagado continua tendo histórico, inclusive a revisão da exclusão com o último estado conhecido. na interface, a página de um post tem um botão que abre essa linha do tempo. os detalhes de como isso funciona por dentro estão em [docs/PERSISTENCIA.md](docs/PERSISTENCIA.md).

## testes

a camada de persistência tem uma suíte de testes automatizados. para rodar, a partir de `backend`:

```bash
./mvnw test
```

são testes de repositório com `@DataJpaTest` (consultas derivadas, restrição de unicidade, valores padrão e travamento otimista) e testes de histórico com `@SpringBootTest`, que exercitam o Envers de ponta a ponta — o ciclo de vida completo de um post (criar, editar, publicar, apagar) e o endpoint de consulta. há ainda um teste de unidade do `GlobalExceptionHandler` cobrindo o mapeamento dos erros de integridade para 409.

## erros

a API responde erro sempre no mesmo formato, com `timestamp`, `status`, `message` e o caminho. os principais casos:

- 400 quando o corpo não passa na validação (traz a lista de campos com problema)
- 404 quando o recurso não existe
- 409 quando uma regra é violada (email de autor repetido) ou quando há conflito de concorrência/integridade na gravação
