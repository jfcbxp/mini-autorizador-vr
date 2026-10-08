# Mini Autorizador — VR Benefícios

Solução do desafio técnico VR Benefícios. API REST para criação de cartões de benefício e autorização de transações de débito.

---

## Stack

- Java 25 + Virtual Threads
- Spring Boot 4.1.0
- Spring Security (HTTP Basic)
- Spring Data JPA + Hibernate (Optimistic Locking)
- Spring Retry
- Flyway
- MySQL 5.7
- BCrypt (hashing de senha)

---

## Como rodar

### Pré-requisitos

- Docker e Docker Compose
- JDK 25
- Maven (ou use a IDE com suporte a Java 25)

### 1. Subir o banco

```bash
cd docker
docker-compose up -d
```

Aguarde o healthcheck do MySQL passar (cerca de 20s). O banco `miniautorizador` é criado automaticamente pelo `MYSQL_DATABASE`.

### 2. Subir a aplicação

```bash
mvn spring-boot:run
```

Ou rode `MiniAutorizadorApplication` diretamente pela IDE.

O Flyway executa `V1__create_cards_table.sql` automaticamente na inicialização e cria a tabela `cards`.

### 3. Verificar

```bash
curl http://localhost:8080/actuator/health
```

---

## Contratos da API

Autenticação: **HTTP Basic** — `username` / `password` em todas as rotas.

### POST /cartoes — Criar cartão

```bash
curl -X POST http://localhost:8080/cartoes \
  -u username:password \
  -H "Content-Type: application/json" \
  -d '{"numeroCartao":"6549873025634501","senha":"1234"}'
```

| Situação | Status | Body |
|----------|--------|------|
| Criado | 201 | `{"numeroCartao":"...","senha":"..."}` |
| Já existe | 422 | `{"numeroCartao":"...","senha":"..."}` |
| Sem auth | 401 | — |

### GET /cartoes/{numeroCartao} — Consultar saldo

```bash
curl http://localhost:8080/cartoes/6549873025634501 \
  -u username:password
```

| Situação | Status | Body |
|----------|--------|------|
| Encontrado | 200 | `500.00` |
| Não existe | 404 | — |
| Sem auth | 401 | — |

### POST /transacoes — Autorizar transação

```bash
curl -X POST http://localhost:8080/transacoes \
  -u username:password \
  -H "Content-Type: application/json" \
  -d '{"numeroCartao":"6549873025634501","senhaCartao":"1234","valor":10.00}'
```

| Situação | Status | Body |
|----------|--------|------|
| Autorizada | 201 | `OK` |
| Saldo insuficiente | 422 | `SALDO_INSUFICIENTE` |
| Senha inválida | 422 | `SENHA_INVALIDA` |
| Cartão inexistente | 422 | `CARTAO_INEXISTENTE` |
| Sem auth | 401 | — |

---

## Decisões de projeto

### Padrões de projeto e princípios de design

Este projeto é um backend REST e não possui um design system visual de interface. No código, os principais padrões e princípios aplicados são:

- **Strategy:** cada validação de autorização implementa `AuthorizationRule`. `TransactionServiceImpl` recebe a lista dessas regras pelo Spring e as executa em ordem. Uma regra pode interromper o fluxo lançando `AuthorizationException`.
- **Chain of Responsibility (orquestração simples):** o serviço percorre as regras ordenadas com `forEach`; não há referências ou encadeamento direto entre elas. O cartão inexistente é tratado antes da lista, na consulta ao repositório.
- **Repository:** `CardRepository`, baseado em Spring Data JPA, abstrai o acesso e a persistência dos cartões.
- **Arquitetura em camadas e DTOs:** controllers tratam HTTP, services coordenam as operações, repositories acessam dados e records de request/response separam o contrato REST da entidade JPA.

Os princípios **SOLID** aparecem de forma pragmática:

- **SRP:** controllers, serviços, repositório e regras têm responsabilidades distintas.
- **OCP:** novas validações podem ser adicionadas como implementações de `AuthorizationRule`, sem alterar o loop que executa as regras.
- **ISP:** `AuthorizationRule` expõe apenas a operação `evaluate` necessária ao fluxo.
- **DIP:** o serviço de transações depende das abstrações `CardRepository` e `AuthorizationRule`, fornecidas por injeção de dependências.

O princípio **LSP** não é um foco explícito da solução; as implementações de `AuthorizationRule` apenas seguem o contrato comum de validar a transação ou lançar a exceção de autorização correspondente.

### Banco de dados — MySQL

MySQL foi escolhido por suporte nativo a transações ACID e ao mecanismo de Optimistic Locking via `@Version`, essencial para a garantia de consistência do saldo sob concorrência.

### Segurança de senha — BCrypt

O PIN do cartão nunca é armazenado em texto plano. Na criação é codificado com `BCryptPasswordEncoder` e na autorização comparado via `matches()`. O PIN nunca aparece em logs.

### Mascaramento de dados sensíveis

O número do cartão é mascarado em todos os logs, exibindo apenas os 4 últimos dígitos: `************4501`.

### Zero ifs no fluxo de autorização

O desafio opcional de eliminar `if`s foi atendido:

- `TransactionServiceImpl.authorize` itera as regras com `forEach` — sem nenhum branch
- `CardExistsRule` usa `Optional.ofNullable(card).ifPresentOrElse(...)`
- `PasswordMatchRule` usa `Optional.of(matches).filter(Boolean::booleanValue).ifPresentOrElse(...)`
- `SufficientBalanceRule` usa `Optional.of(balance >= amount).filter(...).ifPresentOrElse(...)`
- `CardServiceImpl.createCard` usa `findById(...).ifPresent(c -> throw ...)`

Adicionar uma nova regra de autorização requer apenas um novo `@Component @Order(N)` — `TransactionServiceImpl` não precisa ser alterado (Open/Closed Principle).

### Concorrência entre instâncias — Optimistic Locking

O campo `@Version` na entidade `Card` é o único mecanismo de controle de concorrência. Hibernate traduz cada `save` em:

```sql
UPDATE cards SET balance = ?, version = (version + 1)
 WHERE card_number = ? AND version = ?
```

Se duas requisições leram o mesmo `version` e tentam salvar simultaneamente, apenas uma terá `1 row affected`. A outra recebe `OptimisticLockingFailureException`, que é capturada pelo `@Retryable` (até 3 tentativas com backoff de 50ms). Em cada retry o cartão é relido do banco com o saldo atualizado, e as regras são reavaliadas. Se as tentativas se esgotarem, `@Recover` converte a falha em `SALDO_INSUFICIENTE` (HTTP 422), sem permitir saldo negativo.

Nenhum `synchronized`, `ReentrantLock` ou cache JVM é utilizado. O banco é a única fonte de verdade.

#### Por que essa escolha é adequada para este desafio

- Sem infraestrutura adicional — apenas o banco já existente
- Sem deadlocks (ao contrário de `SELECT FOR UPDATE`)
- Correto por construção: o banco rejeita qualquer escrita com `version` desatualizado
- Validado pelo teste k6: 50 VUs concorrentes na mesma instância, zero double-spend, saldo nunca negativo

#### Limitações em alta escala

O Optimistic Locking assume que colisões são **raras**. Quando muitas instâncias disputam o mesmo cartão simultaneamente, a taxa de colisão cresce e o padrão se degrada:

- **Write amplification**: cada colisão gera um retry com nova leitura + nova tentativa de escrita no banco
- **Starvation**: sob contenção extrema, uma thread pode esgotar as 3 tentativas sem conseguir commitar
- **Latência p99**: o p95 pode parecer saudável enquanto o p99 explode por causa dos retries acumulados
- **Pool de conexões**: retries mantêm conexões abertas por mais tempo, reduzindo a capacidade efetiva

#### Caminhos de evolução para alta escala

| Solução | Quando adotar | Trade-off |
|---------|---------------|-----------|
| **Pessimistic Locking** (`SELECT FOR UPDATE`) | Contenção moderada, ainda no mesmo banco | Serializa escritas por cartão, elimina retries, mas cria fila no banco |
| **Redis `DECRBY` atômico** | Alto volume, latência crítica | Operação atômica em memória, sub-milissegundo; requer sincronização eventual com o banco |
| **Fila FIFO por cartão** (ex: SQS FIFO com `MessageGroupId = numeroCartao`) | Picos imprevisíveis, múltiplos consumidores | Serialização perfeita sem contenção; adiciona latência e complexidade operacional |
| **Kafka com partição por cartão** | Volume muito alto, auditoria de eventos | Throughput máximo e ordenação garantida; overkill para domínios simples |

A progressão natural seria: Optimistic Locking → Pessimistic Locking → Redis → Fila, adotando cada nível somente quando o anterior se provar insuficiente sob carga real.

---

## Testes unitários

```bash
# Rodar pela IDE com JDK 25 no PATH
mvn test
```

Cobertura das classes de negócio:

| Classe | Testes |
|--------|--------|
| `CardExistsRule` | card existe / card null |
| `PasswordMatchRule` | senha correta / senha errada |
| `SufficientBalanceRule` | saldo igual / saldo maior / saldo insuficiente |
| `CardServiceImpl` | criação / duplicado / saldo encontrado / não encontrado |
| `TransactionServiceImpl` | autorizada / regra falha / cartão null |
| `CardController` | 201 / 422 / 400 / 401 / 200 / 404 |
| `TransactionController` | 201 / 422 (3 erros) / 401 / 400 |

---

## Teste de performance — k6

### Pré-requisitos

```bash
winget install k6
```

### Executar

```bash
k6 run stress-test.js
```

### O que o teste valida

O script `stress-test.js` prepara um cartão e executa um cenário de carga:

1. **Setup** — cria um cartão único para cada execução, debita R$490,00 do saldo inicial e verifica que restam R$10,00 antes de começar a carga
2. **concurrent_transactions** — 50 VUs simultâneas durante 30s, cada uma tentando debitar R$1,00 do mesmo cartão

**Critérios do teste:**
- `transactions_unexpected == 0` — nenhuma resposta fora de 201 ou 422 é tolerada
- `http_req_duration{scenario:concurrent_transactions}` `p(95) < 500ms` — referência interna de latência para as requisições do cenário de carga; respostas 422 esperadas não contam como falhas HTTP

**Resultado esperado:**
- `transactions_ok: 10` — exatamente as 10 transações que cabem no saldo passam
- `transactions_saldo_insuficiente: ~1530` — todas as demais são bloqueadas
- Saldo final: `R$0.00` — nunca negativo, zero double-spend

O contador de transações autorizadas considera apenas o cenário de carga, não as requisições de preparação. O teste exercita 50 VUs concorrentes contra uma instância da aplicação; ele não simula múltiplas instâncias.

A meta de p95 de 500 ms é apenas uma referência de performance deste projeto, não um requisito da proposta da vaga. Se essa referência não for atingida, o k6 sinaliza a falha do threshold, mas isso não significa que os requisitos funcionais da proposta falharam.

---

## Observabilidade — Dynatrace

Com o OneAgent instalado, o Dynatrace captura automaticamente traces distribuídos, métricas de JVM e chamadas ao banco. Abaixo as métricas e alertas recomendados para este serviço.

### Métricas de negócio (Calculated Metrics)

Criar via **Settings → Server-side service monitoring → Calculated service metrics**:

| Nome | Tipo | Filtro | O que monitora |
|------|------|--------|----------------|
| `transacoes_autorizadas` | Request count | URL = `/transacoes` + HTTP 201 | Volume de transações aprovadas por minuto |
| `transacoes_negadas_saldo` | Request count | URL = `/transacoes` + response body contains `SALDO_INSUFICIENTE` | Rejeições por saldo insuficiente |
| `transacoes_negadas_senha` | Request count | URL = `/transacoes` + response body contains `SENHA_INVALIDA` | Tentativas com senha errada (fraude?) |
| `transacoes_negadas_cartao` | Request count | URL = `/transacoes` + response body contains `CARTAO_INEXISTENTE` | Cartões inválidos na requisição |
| `cartoes_criados` | Request count | URL = `/cartoes` + HTTP 201 | Novos cartões emitidos |
| `cartoes_duplicados` | Request count | URL = `/cartoes` + HTTP 422 | Tentativas de criação duplicada |
| `optimistic_lock_retries` | Request count | Exception = `OptimisticLockingFailureException` | Colisões de concorrência — indica carga alta |

### Alertas recomendados (Anomaly Detection)

| Alerta | Condição | Severidade | Justificativa |
|--------|----------|------------|---------------|
| Taxa de erros 5xx | `> 1%` por 5 min | Critical | Qualquer 500 indica bug — zero é esperado |
| Latência p95 `/transacoes` | `> 500ms` por 3 min | High | Threshold do k6 — degradação perceptível |
| `optimistic_lock_retries` | `> 50/min` por 5 min | Medium | Contenção excessiva no banco |
| `transacoes_negadas_senha` | aumento `> 200%` em 5 min | High | Possível ataque de força bruta |
| Disponibilidade do serviço | `< 99.9%` | Critical | SLA mínimo |

### Dashboard sugerido

Um único dashboard com 4 tiles cobre o essencial:

1. **Funil de transações** — `autorizadas` vs `saldo_insuficiente` vs `senha_invalida` vs `cartao_inexistente` (bar chart por minuto)
2. **Latência** — p50 / p95 / p99 de `/transacoes` e `/cartoes` (line chart)
3. **Saúde JVM** — heap used, GC pause time, virtual threads ativos
4. **Banco** — connection pool wait time, query duration p95, `optimistic_lock_retries`

### Pré-requisito: expor métricas do Actuator

O Dynatrace OneAgent lê automaticamente o endpoint `/actuator/metrics` se o Spring Boot Actuator estiver no classpath. Para habilitar todas as métricas:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      dynatrace:
        enabled: true
```

---

## Suposições documentadas

- O número do cartão é tratado como string (sem validação de formato Luhn)
- A transação não é persistida — apenas o saldo do cartão é atualizado
- O saldo inicial de R$500,00 é fixo e não configurável
- Após 3 falhas consecutivas por `OptimisticLockingFailureException`, a transação é rejeitada com `SALDO_INSUFICIENTE` (outra instância já consumiu o saldo)
