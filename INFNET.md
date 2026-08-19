# Terceira Entrega: Criação de um Microsserviço

No curso de Engenharia de Softwares Escaláveis, esta terceira etapa envolve expandir a aplicação monolítica anterior, introduzindo um microsserviço usando Spring Boot e Spring Cloud. Este novo componente deve se integrar de forma eficaz ao sistema maior, exemplificando práticas de desenvolvimento distribuído.

## Objetivo da Etapa

Desenvolver e integrar um microsserviço, demonstrando competência em separação de responsabilidades, comunicação entre serviços e modularização.

## Subcompetências a serem Desenvolvidas

### 1. Atualização do Modelo de Domínio

Incorporar um novo serviço no modelo de domínio, refletindo mudanças na arquitetura e nas dependências.

### 2. Criação de Endpoints da API REST

Implementar novos endpoints para acesso ao microsserviço via REST API.

### 3. Implementação de Microsserviço

- Usar Spring Boot para o desenvolvimento do microsserviço.
- Aplicar Spring Cloud para facilitar a configuração e comunicação distribuídas.

### 4. Desenvolvimento de Repositórios

Criar repositórios dedicados para gerenciar dados específicos do microsserviço.

### 5. Desenvolvimento de Componentes Front-End

Adicionar componentes na interface do usuário para interação com o novo microsserviço.

### 6. Atualização e Criação de Testes

Expandir a cobertura de testes para incluir o novo microsserviço e atualizações no sistema.

## Entregas Esperadas

- **Código Fonte:** Repositório com o microsserviço e atualizações correspondentes no sistema e interface de usuário.
- **Documentação:** Arquitetura detalhada do microsserviço, integração com o sistema, e descrição dos novos endpoints da API.
- **Demonstração de Funcionalidade:** Apresentação prática mostrando a operação integrada do novo microsserviço.

## Avaliação

A avaliação considerará a funcionalidade do microsserviço, qualidade de implementação, adequação dos testes, e a clareza da documentação arquitetural.

Este projeto aumenta a complexidade do sistema e enriquece a experiência prática dos alunos com arquiteturas modernas de software, preparando-os para enfrentar desafios em ambientes de software distribuídos e dinâmicos.

---

## Onde cada item foi atendido nesta solução

| Subcompetência | Onde está |
| --- | --- |
| Atualização do modelo de domínio | novo aggregate `Reaction` e o `Comment` migrado, em [engagement-service/src/main/java/com/blog/engagement/domain/](engagement-service/src/main/java/com/blog/engagement/domain/); modelo e dependências redesenhados em [docs/MICROSSERVICO.md](docs/MICROSSERVICO.md) e [docs/ARQUITETURA.md](docs/ARQUITETURA.md) |
| Endpoints da API REST | rotas de reação e de status no monólito ([ReactionController](backend/src/main/java/com/blog/engagement/web/ReactionController.java), [EngagementStatusController](backend/src/main/java/com/blog/engagement/web/EngagementStatusController.java)) e a API própria do microsserviço ([engagement-service/.../web/](engagement-service/src/main/java/com/blog/engagement/web/)); tabelas em [README.md](README.md) e [docs/MICROSSERVICO.md](docs/MICROSSERVICO.md) |
| Microsserviço com Spring Boot | projeto [engagement-service/](engagement-service/), processo próprio na porta 8081 com banco próprio |
| Spring Cloud — comunicação distribuída | Eureka em [discovery-server/](discovery-server/), OpenFeign + LoadBalancer + Resilience4j no [EngagementClient](backend/src/main/java/com/blog/engagement/client/EngagementClient.java) |
| Spring Cloud — configuração distribuída | Config Server em [config-server/](config-server/), com as propriedades de ambiente dos dois serviços em [config-server/src/main/resources/config/](config-server/src/main/resources/config/) |
| Repositórios dedicados | [CommentRepository](engagement-service/src/main/java/com/blog/engagement/repository/CommentRepository.java) e [ReactionRepository](engagement-service/src/main/java/com/blog/engagement/repository/ReactionRepository.java), com consulta de agregação e projeção por interface |
| Componentes front-end | [ReactionBar.jsx](frontend/src/components/ReactionBar.jsx), [ServiceBadge.jsx](frontend/src/components/ServiceBadge.jsx) e a degradação graciosa em [PostPage.jsx](frontend/src/pages/PostPage.jsx) |
| Testes | 93 testes nos quatro serviços; estratégia descrita em [docs/MICROSSERVICO.md](docs/MICROSSERVICO.md) |
| Demonstração | [subir.sh](subir.sh) sobe a stack em um comando; roteiro passo a passo na seção "demonstração" de [docs/MICROSSERVICO.md](docs/MICROSSERVICO.md) |

---

## Template de Rubrica para ser utilizado com a extensão Rubricator

### 3. Criar um microsserviço usando Spring Boot e Spring Cloud, integrando-o a uma aplicação existente

**O aluno atualizou o modelo de domínio para incorporar o novo serviço, refletindo as mudanças na arquitetura e nas dependências?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno implementou novos endpoints da API REST para acesso ao microsserviço?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno usou Spring Boot para o desenvolvimento do microsserviço?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno aplicou Spring Cloud para facilitar a configuração e a comunicação distribuídas?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno criou repositórios dedicados para gerenciar os dados específicos do microsserviço?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno adicionou componentes na interface do usuário para interação com o novo microsserviço?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno expandiu a cobertura de testes para incluir o novo microsserviço e as atualizações no sistema?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno demonstrou separação de responsabilidades entre o monólito e o microsserviço?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno implementou a comunicação entre os serviços de forma eficaz?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno atualizou o repositório Git com o microsserviço e as alterações correspondentes no sistema e na interface?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno documentou a arquitetura do microsserviço e a sua integração com o sistema?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno descreveu os novos endpoints da API na documentação?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

**O aluno apresentou uma demonstração prática da operação integrada do novo microsserviço?**

- Não demonstrou o item de rubrica
- Demonstrou o item de rubrica

---

## Entregas anteriores

### Segunda Entrega: Desenvolver uma Camada de Persistência Real

Implementar uma camada de persistência que não apenas suporte as operações básicas de CRUD, mas também introduza funcionalidades avançadas como o histórico de dados, utilizando as capacidades do JPA e dos Repositórios Spring Data. As subcompetências cobriam modelagem de dados, integração de JPA com Spring Data, gerenciamento de dados, integração de funcionalidades de histórico e implementação de testes.

O resultado está documentado em [docs/PERSISTENCIA.md](docs/PERSISTENCIA.md).

### Primeira Entrega: Base em Camadas e Bounded Contexts

Montar a estrutura do monólito separada por camadas (controller, service, repository) e por bounded contexts, preparando o terreno para a evolução para microsserviços.

O resultado está documentado em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).
