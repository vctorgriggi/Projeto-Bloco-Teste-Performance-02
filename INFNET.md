# Quarta Entrega: Refatoração para Arquitetura Orientada a Eventos

## Objetivo da Etapa

Refatorar o sistema atual para adotar uma arquitetura orientada a eventos, utilizando RabbitMQ como message broker para facilitar a comunicação baseada em eventos entre os componentes do sistema.

## Subcompetências a serem Desenvolvidas

### 1. Avaliação de Arquitetura Orientada a Eventos

Apresentar os prós e contras da arquitetura orientada a eventos, identificando cenários onde ela é mais vantajosa.

### 2. Desenvolvimento de Padrões de Mensagens

Criar diferentes padrões de mensagens para atender diversos casos de uso, assegurando a eficácia da comunicação entre componentes.

### 3. Implementação com RabbitMQ

Implementar padrões de arquitetura orientada a eventos usando RabbitMQ, explorando suas capacidades como message broker.

### 4. Simplificação com Spring Boot

Utilizar abstrações fornecidas pelo Spring Boot para facilitar a implementação de mensagens e integração com RabbitMQ.

### 5. Refatoração do Sistema

Converter um sistema previamente acoplado em uma arquitetura orientada a eventos, focando em melhorar a escalabilidade, resiliência e eficiência na gestão de transações.

## Entregas Esperadas

- **Código Fonte:** Repositório com o código do sistema refatorado para utilizar uma arquitetura orientada a eventos.
- **Documentação:** Documentação detalhada explicando as mudanças implementadas, incluindo diagramas de arquitetura e fluxos de eventos.
- **Demonstração de Funcionalidade:** Apresentação prática do sistema refatorado, demonstrando a eficácia da nova arquitetura em cenários simulados.

## Avaliação

Os alunos serão avaliados com base na qualidade da implementação da arquitetura orientada a eventos, a eficiência da integração com RabbitMQ, a robustez dos padrões de mensagens desenvolvidos, e a clareza da documentação.

Este projeto permite aos alunos aplicar conceitos avançados de arquitetura de sistemas em um contexto prático, preparando-os para enfrentar desafios de sistemas distribuídos em ambientes corporativos reais.

---

## Onde cada item foi atendido nesta solução

| Subcompetência | Onde está |
| --- | --- |
| Avaliação de arquitetura orientada a eventos | prós, contras, quando vale e quando não vale, e o critério aplicado interação por interação, em [docs/EVENTOS.md](docs/EVENTOS.md) (seções "prós e contras" e "o critério usado aqui") |
| Padrões de mensagens | quatro padrões de mensagem — notificação de evento (`post.deleted`), comando ponto a ponto com consumidores concorrentes (`comment.register`), transferência de estado pelo evento (`comment.*`, `reaction.*`, `engagement.*`) e pedido assíncrono de republicação (`engagement.snapshot.request`) — e três de confiabilidade: outbox transacional, consumidor idempotente, retentativa com dead letter e alternate exchange. catálogo e formato em [docs/EVENTOS.md](docs/EVENTOS.md) |
| Implementação com RabbitMQ | topologia com exchanges topic, direct e fanout, filas duráveis, dead letter exchange, alternate exchange e publisher confirms, declarada em [MessagingConfig do monólito](backend/src/main/java/com/blog/shared/messaging/MessagingConfig.java) e [do microsserviço](engagement-service/src/main/java/com/blog/engagement/messaging/MessagingConfig.java); broker em [docker-compose.yml](docker-compose.yml) |
| Simplificação com Spring Boot | `spring-boot-starter-amqp`, `@RabbitListener`, `Declarables`/`RabbitAdmin`, retentativa por propriedade com `RabbitRetryTemplateCustomizer`, `RabbitTemplateCustomizer`, confirmações por propriedade, `@TransactionalEventListener` e `@ServiceConnection` nos testes; tabela em [docs/EVENTOS.md](docs/EVENTOS.md) (seção "o que o spring boot simplificou") |
| Refatoração do sistema | o comentário virou comando em fila ([CommentService](backend/src/main/java/com/blog/engagement/service/CommentService.java), [CommentCommandListener](engagement-service/src/main/java/com/blog/engagement/messaging/CommentCommandListener.java)); a limpeza de post apagado virou evento via outbox ([PostEventsOutbox](backend/src/main/java/com/blog/authoring/messaging/PostEventsOutbox.java), [OutboxRelay](backend/src/main/java/com/blog/shared/messaging/outbox/OutboxRelay.java), [PostDeletedListener](engagement-service/src/main/java/com/blog/engagement/messaging/PostDeletedListener.java)); os contadores da estante vêm de uma projeção alimentada por eventos ([EngagementSnapshotListener](backend/src/main/java/com/blog/engagement/messaging/EngagementSnapshotListener.java), [EngagementEventPublisher](engagement-service/src/main/java/com/blog/engagement/messaging/EngagementEventPublisher.java)). o efeito em escalabilidade, resiliência e transações está em [docs/EVENTOS.md](docs/EVENTOS.md) (seção "o que melhorou") |
| Código fonte | este repositório; a interface acompanhou a mudança em [PostPage.jsx](frontend/src/pages/PostPage.jsx) (recado "na fila"), [HomePage.jsx](frontend/src/pages/HomePage.jsx) (contadores) e [ServiceBadge.jsx](frontend/src/components/ServiceBadge.jsx) (selo da fila) |
| Documentação com diagramas de arquitetura e fluxos de eventos | [docs/EVENTOS.md](docs/EVENTOS.md): diagrama da topologia, três diagramas de sequência dos fluxos de eventos, catálogo de mensagens e tabela de falhas; visão geral atualizada em [docs/ARQUITETURA.md](docs/ARQUITETURA.md) |
| Testes | 133 testes nos quatro serviços (eram 93), incluindo integração com um RabbitMQ real em container (Testcontainers); estratégia em [docs/EVENTOS.md](docs/EVENTOS.md) |
| Demonstração em cenários simulados | [subir.sh](subir.sh) sobe o broker e a stack em um comando; o roteiro em [docs/EVENTOS.md](docs/EVENTOS.md) simula a queda do microsserviço, a queda do broker, a mensagem venenosa, o evento sem assinante e duas instâncias dividindo a fila |

---

## Entregas anteriores

### Terceira Entrega: Criação de um Microsserviço

Expandir a aplicação monolítica introduzindo um microsserviço com Spring Boot e Spring Cloud, integrado de forma eficaz ao sistema maior. As subcompetências cobriam a atualização do modelo de domínio, novos endpoints REST, a implementação do microsserviço com Spring Boot e Spring Cloud (configuração e comunicação distribuídas), repositórios dedicados, componentes de front-end e a expansão dos testes.

O resultado está documentado em [docs/MICROSSERVICO.md](docs/MICROSSERVICO.md).

### Segunda Entrega: Desenvolver uma Camada de Persistência Real

Implementar uma camada de persistência que não apenas suporte as operações básicas de CRUD, mas também introduza funcionalidades avançadas como o histórico de dados, utilizando as capacidades do JPA e dos Repositórios Spring Data. As subcompetências cobriam modelagem de dados, integração de JPA com Spring Data, gerenciamento de dados, integração de funcionalidades de histórico e implementação de testes.

O resultado está documentado em [docs/PERSISTENCIA.md](docs/PERSISTENCIA.md).

### Primeira Entrega: Base em Camadas e Bounded Contexts

Montar a estrutura do monólito separada por camadas (controller, service, repository) e por bounded contexts, preparando o terreno para a evolução para microsserviços.

O resultado está documentado em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).
