# Última Entrega: Implantação e Manutenção em Produção

## Objetivo da Etapa

Preparar o sistema desenvolvido para operação através de conteinerização, monitoramento e testes.

## Subcompetências a serem Desenvolvidas

### 1. Implantação com Docker e Kubernetes

- Utilizar Docker para conteinerização dos microsserviços do sistema.
- Empregar Kubernetes para orquestrar a implantação e escalabilidade dos contêineres em um ambiente de produção.

### 2. Monitoramento de Microsserviços

Configurar ferramentas de agregação de logs e rastreamento de transações para monitorar a operação dos microsserviços, facilitando a detecção e resolução de problemas.

### 3. Gestão de Configuração e Versionamento

Usar Git e GitHub para controlar versões e documentar as mudanças no projeto, promovendo uma gestão eficaz do código fonte e colaboração entre desenvolvedores.

### 4. Automação com GitHub Actions

Configurar e utilizar GitHub Actions para automatizar o processo de integração contínua (CI) e entrega contínua (CD), visando uma operação mais fluida e menos suscetível a erros.

### 5. Testes Abrangentes

Assegurar que todos os componentes do sistema funcionem corretamente tanto individualmente quanto em conjunto, desenvolvendo e aplicando testes abrangentes que cobrem diversos aspectos do sistema.

## Entregas Esperadas

- **Código Fonte:** Código dos microsserviços adaptados para operação em Docker/Kubernetes, com configurações necessárias para a implantação.
- **Documentação:** Documentação atualizada incluindo detalhes de implantação, configuração de monitoramento, e processos de CI/CD.
- **Demonstração de Operação:** Apresentação mostrando a implantação, monitoramento, e funcionamento dos microsserviços em um ambiente simulado de produção.

## Avaliação

A avaliação será baseada na correta implantação dos sistemas, eficácia das práticas de monitoramento e gestão de configuração, qualidade da automação de CI/CD, e a robustez dos testes implementados.

---

## Onde cada item foi atendido nesta solução

| Subcompetência | Onde está |
| --- | --- |
| Docker | um [Dockerfile](backend/Dockerfile) por serviço (multi-stage, JRE Alpine, usuário sem root, camadas do Spring Boot, agente OpenTelemetry com checksum fixo) e o [do front](frontend/Dockerfile) (nginx sem root); o ambiente simulado de produção em [docker-compose.yml](docker-compose.yml) (`--profile completo`), com PostgreSQL, RabbitMQ e observabilidade |
| Kubernetes | manifestos em [deploy/](deploy/) (kustomize): StatefulSets com volume para PostgreSQL e RabbitMQ, sondas de startup, liveness e readiness, Secrets, rollout sem queda, autoescalonamento (HPA de 2 a 4 réplicas) e PodDisruptionBudget no engajamento; cluster kind em [scripts/k8s-subir.sh](scripts/k8s-subir.sh). rollout medido com 0 erro em 848 requisições; escala sob carga com 0 falha em 48.175 |
| Agregação de logs e rastreamento | agente OpenTelemetry nas imagens e a stack Grafana LGTM: logs no Loki com o id do trace, traces no Tempo atravessando HTTP, RabbitMQ e o outbox, métricas no Prometheus (incluindo profundidade das filas e eventos pendentes no outbox), [dashboard "Blog — operação"](deploy/grafana/dashboards/blog-operacao.json) e [descarte de ruído no coletor](deploy/otel/otelcol-config.yaml) |
| Gestão de configuração e versionamento | perfil `container` no config server (um arquivo para compose e Kubernetes, senhas resolvidas no pod); migrações Flyway versionadas; commits convencionais, tags por entrega, [CHANGELOG.md](CHANGELOG.md), [template de PR](.github/pull_request_template.md) e [Dependabot](.github/dependabot.yml) |
| GitHub Actions (CI/CD) | [pipeline.yml](.github/workflows/pipeline.yml): testes com cobertura (em paralelo por serviço), validação de manifestos, compose e workflow, imagens, e2e num cluster Kubernetes efêmero, publicação no GHCR, manifesto versionado por commit, release em tag e implantação condicionada a um cluster configurado |
| Testes abrangentes | 138 testes Java (unidade e integração, inclusive com RabbitMQ real via Testcontainers) e 15 do front (Vitest); [e2e](scripts/e2e.sh) contra a stack implantada (16 verificações, incluindo traces e logs); [teste de carga k6](scripts/carga.js); cobertura JaCoCo; validação de manifestos (kubeconform) |
| Documentação | [docs/IMPLANTACAO.md](docs/IMPLANTACAO.md): implantação, configuração, monitoramento, CI/CD, testes, os defeitos que a implantação revelou e o roteiro de demonstração |
| Demonstração de operação | roteiro em [docs/IMPLANTACAO.md](docs/IMPLANTACAO.md): implantação, e2e, trace atravessando a fila, atualização sem queda, escala sob carga, pod morto e broker fora do ar |

---

## Entregas anteriores

### Quarta Entrega: Refatoração para Arquitetura Orientada a Eventos

Refatorar o sistema para uma arquitetura orientada a eventos, usando RabbitMQ como message broker. As subcompetências cobriam a avaliação da arquitetura orientada a eventos (prós, contras e cenários), o desenvolvimento de padrões de mensagens, a implementação com RabbitMQ, a simplificação com Spring Boot e a conversão do sistema acoplado, com foco em escalabilidade, resiliência e gestão de transações.

O resultado está documentado em [docs/EVENTOS.md](docs/EVENTOS.md).

### Terceira Entrega: Criação de um Microsserviço

Expandir a aplicação monolítica introduzindo um microsserviço com Spring Boot e Spring Cloud, integrado de forma eficaz ao sistema maior. As subcompetências cobriam a atualização do modelo de domínio, novos endpoints REST, a implementação do microsserviço com Spring Boot e Spring Cloud (configuração e comunicação distribuídas), repositórios dedicados, componentes de front-end e a expansão dos testes.

O resultado está documentado em [docs/MICROSSERVICO.md](docs/MICROSSERVICO.md).

### Segunda Entrega: Desenvolver uma Camada de Persistência Real

Implementar uma camada de persistência que não apenas suporte as operações básicas de CRUD, mas também introduza funcionalidades avançadas como o histórico de dados, utilizando as capacidades do JPA e dos Repositórios Spring Data. As subcompetências cobriam modelagem de dados, integração de JPA com Spring Data, gerenciamento de dados, integração de funcionalidades de histórico e implementação de testes.

O resultado está documentado em [docs/PERSISTENCIA.md](docs/PERSISTENCIA.md).

### Primeira Entrega: Base em Camadas e Bounded Contexts

Montar a estrutura do monólito separada por camadas (controller, service, repository) e por bounded contexts, preparando o terreno para a evolução para microsserviços.

O resultado está documentado em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).
