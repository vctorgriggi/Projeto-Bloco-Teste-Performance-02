# changelog

o que mudou em cada entrega, da mais recente para a mais antiga. o formato segue o
[keep a changelog](https://keepachangelog.com/pt-BR/1.1.0/), e cada entrega e uma versao
com tag no git (`v5.0.0`, `v4.0.0`, ...). os commits seguem o
[conventional commits](https://www.conventionalcommits.org/pt-br/v1.0.0/) (`feat:`,
`fix:`, `docs:`, `chore:`), e o corpo de cada um explica o porque, nao so o que.

## [5.0.0] - quinta entrega: implantacao e operacao

### adicionado
- imagem docker de cada servico (multi-stage, jre alpine, usuario sem root com uid fixo,
  camadas do spring boot, agente opentelemetry com checksum fixo) e do front (nginx sem
  root, repassando `/api` ao monolito)
- `docker compose --profile completo`: o ambiente simulado de producao, com postgres,
  rabbitmq e a stack de observabilidade
- implantacao no kubernetes (`deploy/`, kustomize): StatefulSets para postgres e rabbitmq,
  sondas de startup/liveness/readiness, duas replicas do engajamento com autoescalonamento
  (hpa) e PodDisruptionBudget, e o cluster kind da demonstracao (`scripts/k8s-subir.sh`)
- observabilidade com opentelemetry e grafana lgtm: traces que atravessam http, rabbitmq
  e o outbox; logs no loki com o id do trace; metricas de http, jvm, pool de conexoes,
  profundidade das filas e eventos pendentes no outbox; dashboard "Blog — operacao"
- perfil `container` no config server: um arquivo de configuracao para compose e
  kubernetes, com as credenciais resolvidas no pod (Secrets), nunca no repositorio
- postgresql com migracoes flyway (`V1__schema_inicial.sql`) e `ddl-auto: validate`
- pipeline no github actions: testes com cobertura, validacao de manifestos, imagens,
  e2e num cluster kind efemero, publicacao no ghcr e manifesto versionado por commit
- teste de ponta a ponta (`scripts/e2e.sh`), teste de carga k6 (`scripts/carga.js`) e
  testes do front (vitest)
- dependabot, template de pull request e este changelog

### corrigido
- texto de post ou comentario com mais de 255 caracteres quebrava a gravacao: o `@Lob`
  nao chegava as tabelas de auditoria do envers, que nasciam `varchar(255)`. o defeito
  existia desde a segunda entrega, escondido pelos textos curtos de exemplo, e apareceu
  ao gerar o schema do postgres
- 28% de 503 durante a troca de versao do engajamento no kubernetes: os caches de
  descoberta (servidor eureka, cliente e load balancer) mantinham a replica encerrada na
  lista. resolvido com caches curtos, `preStop` que marca a instancia DOWN no eureka e
  `minReadySeconds` -- zero erro em 848 requisicoes durante o rollout
- o hpa oscilava ao fim de uma rajada (descia para 2 e subia para 4 em 15s)
- com o broker fora, a reacao era gravada e respondida com 503 em 3,9s: o evento de
  melhor esforco era publicado na thread da requisicao. agora sai de um executor proprio
- na implantacao do zero, os servicos subiam antes do postgres e reiniciavam ate dar
  certo; agora esperam num initContainer
- o broker morria na primeira subida com volume novo: a sonda, como root, criava o
  cookie do erlang antes do servidor. agora o conteiner roda como o usuario rabbitmq
- o nginx do front recusava a propria sonda de saude (localhost resolvendo para ipv6)
- a exclusao de um post aparecia como dois traces sem relacao: o outbox agora guarda o
  contexto do trace e o relay publica dentro dele

## [4.0.0] - quarta entrega: arquitetura orientada a eventos

### adicionado
- rabbitmq com topologia declarada pelos servicos: exchanges topic, direct e fanout,
  filas duraveis, dead letter por fila, alternate exchange
- comentario como comando em fila (202), post apagado como evento via outbox
  transacional, contadores da estante por transferencia de estado
- consumidores idempotentes, retentativa com espera exponencial, dead letter imediata
  para mensagem invalida ou ilegivel
- testes de integracao com rabbitmq real (testcontainers)

### corrigido
- consumidores ligavam antes do seeder; o broker fora tirava o engajamento do eureka;
  mensagem ilegivel passava pelas retentativas (detalhes em `docs/EVENTOS.md`)

## [3.0.0] - terceira entrega: microsservico

### adicionado
- `engagement-service`, com banco proprio, e as reacoes como capacidade nova
- spring cloud: eureka, openfeign, loadbalancer, resilience4j e config server

## [2.0.0] - segunda entrega: persistencia

### adicionado
- indices alinhados as consultas, travamento otimista e historico com hibernate envers

## [1.0.0] - primeira entrega: base em camadas

### adicionado
- monolito spring boot em camadas e bounded contexts, com front em react
