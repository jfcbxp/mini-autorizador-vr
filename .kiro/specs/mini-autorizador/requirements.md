# Requirements Document

## Introduction

O Mini Autorizador é um sistema REST que processa transações de cartões de benefício (Vale Refeição / Vale Alimentação). Ele expõe três endpoints para criação de cartões, consulta de saldo e autorização de transações, garantindo controle transacional e corretude sob carga concorrente. O sistema é construído com Java 25 (Virtual Threads), Spring Boot 4.x e armazenamento persistente em banco relacional ou não-relacional (a ser selecionado), com autenticação HTTP Basic obrigatória em todos os endpoints.

---

## Glossary

- **Authorizer**: O sistema Spring Boot que processa requisições REST de cartões e transações.
- **Card**: Entidade persistida que representa um cartão de benefício, identificado por `cardNumber` e protegido por `password`, com saldo inicial de R$500,00.
- **Transaction**: Operação de débito solicitada para um cartão; não é persistida.
- **Balance**: Valor monetário disponível em um Card, armazenado com precisão decimal (BigDecimal).
- **BasicAuth**: Mecanismo de autenticação HTTP Basic com credenciais fixas `username` / `password`.
- **AuthorizationRule**: Conjunto de condições que devem ser satisfeitas para que uma Transaction seja aprovada.
- **TransactionError**: Enum com os valores `CARD_NOT_FOUND`, `INVALID_PASSWORD` e `INSUFFICIENT_BALANCE`, retornado no corpo da resposta HTTP 422.
- **VirtualThread**: Thread virtual da JVM (Project Loom / Java 25), configurada via `spring.threads.virtual.enabled: true` como executor padrão do servidor web.
- **OptimisticLocking**: Estratégia de controle de concorrência baseada em versão, que rejeita atualizações conflitantes sem bloquear o registro durante a leitura.

---

## Requirements

### Requirement 1: Mandatory Authentication on All Endpoints

**User Story:** As a system operator, I want all Authorizer endpoints to be protected by HTTP Basic authentication, so that only authorized clients can create cards, check balances, and perform transactions.

#### Acceptance Criteria

1. THE Authorizer SHALL require HTTP Basic authentication with credentials `username` / `password` (exact match, case-sensitive) on all business endpoints (card creation, balance query, and transaction authorization).
2. IF a request is received without an authorization header, with a malformed authorization header, or with credentials other than `username` / `password`, THEN THE Authorizer SHALL return HTTP 401 with no response body, without processing the requested operation.
3. WHEN a request authenticated with valid credentials is received, THE Authorizer SHALL forward the request to the corresponding business endpoint without modifying or removing the authentication context.

---

### Requirement 2: Card Creation

**User Story:** As an API client, I want to create a new benefit card with a number and password, so that it becomes available for transactions with an initial balance of R$500.00.

#### Acceptance Criteria

1. WHEN a POST `/cartoes` request is received with body `{"numeroCartao": "<number>", "senha": "<password>"}` with non-null and non-empty fields and valid authentication, THE Authorizer SHALL persist the Card with an initial Balance of R$500.00 and return HTTP 201 with body `{"numeroCartao": "<number>", "senha": "<password>"}`.
2. WHEN a POST `/cartoes` request is received with a `numeroCartao` that already exists in the database, THE Authorizer SHALL return HTTP 422 with body `{"numeroCartao": "<number>", "senha": "<password>"}` without altering the existing Card (balance and password remain unchanged).
3. THE Authorizer SHALL create every Card with an initial Balance exactly equal to R$500.00 (BigDecimal `500.00`).
4. IF the body of the POST `/cartoes` request does not contain the fields `numeroCartao` and `senha`, or if any of these fields is null or empty, THEN THE Authorizer SHALL return HTTP 400 without persisting any data.
5. IF the POST `/cartoes` request is received without authentication credentials or with invalid credentials, THEN THE Authorizer SHALL return HTTP 401 with no body and without processing the card creation.

---

### Requirement 3: Card Balance Query

**User Story:** As an API client, I want to query the current balance of a card by its number, so that I can verify how much is available for use.

#### Acceptance Criteria

1. WHEN a GET `/cartoes/{numeroCartao}` request is received with valid authentication and the Card exists in the database, THE Authorizer SHALL return HTTP 200 with body containing the current Balance as a numeric value with exactly 2 decimal places (e.g., `495.15`).
2. WHEN a GET `/cartoes/{numeroCartao}` request is received and the Card does not exist in the database, THE Authorizer SHALL return HTTP 404 with no body.
3. IF the GET `/cartoes/{numeroCartao}` request is received without authentication credentials or with invalid credentials, THEN THE Authorizer SHALL return HTTP 401 with no body.
4. THE Authorizer SHALL ensure that the returned Balance is greater than or equal to 0.00.

---

### Requirement 4: Transaction Authorization

**User Story:** As an API client, I want to submit a debit transaction providing card number, password, and amount, so that the system validates and debits the balance if all rules are met.

#### Acceptance Criteria

1. WHEN a POST `/transacoes` request is received with body `{"numeroCartao": "<number>", "senhaCartao": "<password>", "valor": <amount>}` where `valor > 0`, valid authentication, existing Card, correct password, and sufficient Balance, THE Authorizer SHALL atomically debit the amount from the Card Balance and return HTTP 201 with body `OK`.
2. WHEN a POST `/transacoes` request is received and `numeroCartao` does not match any persisted Card, THE Authorizer SHALL return HTTP 422 with body `CARD_NOT_FOUND` without altering any data.
3. WHEN a POST `/transacoes` request is received and `senhaCartao` does not match the Card password, THE Authorizer SHALL return HTTP 422 with body `INVALID_PASSWORD` without altering the Balance.
4. WHEN a POST `/transacoes` request is received and the transaction `valor` exceeds the Card available Balance, THE Authorizer SHALL return HTTP 422 with body `INSUFFICIENT_BALANCE` without altering the Balance.
5. THE Authorizer SHALL evaluate AuthorizationRules in the following precedence order: `CARD_NOT_FOUND` → `INVALID_PASSWORD` → `INSUFFICIENT_BALANCE`.
6. THE Authorizer SHALL not persist any Transaction record; only the Card Balance is updated.
7. IF the POST `/transacoes` request is received without authentication credentials or with invalid credentials, THEN THE Authorizer SHALL return HTTP 401 with no body and without altering any data.
8. IF the body of the POST `/transacoes` request is malformed, absent, or has fields with incorrect types, THEN THE Authorizer SHALL return HTTP 400 without altering any data.
9. IF the `valor` field of the POST `/transacoes` request is less than or equal to zero, THEN THE Authorizer SHALL return HTTP 422 with body `INVALID_AMOUNT` without altering any data.

---

### Requirement 5: Concurrency Control on Simultaneous Transactions

**User Story:** As a system architect, I want the Authorizer to guarantee atomicity in reading and writing the balance, so that two simultaneous transactions for the same card do not debit more than the available balance.

#### Acceptance Criteria

1. WHEN two POST `/transacoes` requests for the same `numeroCartao` with `valor` equal to the current Balance are processed simultaneously, THE Authorizer SHALL approve exactly one transaction and reject the other with `INSUFFICIENT_BALANCE`.
2. THE Authorizer SHALL ensure that the Balance update is atomic through Card record versioning — no external client should observe an intermediate or inconsistent Balance between the read and the write.
3. WHEN a version conflict is detected during Balance update, THE Authorizer SHALL retry the operation (re-read + re-evaluate rules) up to a maximum of 3 times before returning `INSUFFICIENT_BALANCE`; if the conflict persists after 3 retries, THE Authorizer SHALL return HTTP 422 with body `INSUFFICIENT_BALANCE`.
4. THE Authorizer SHALL ensure that the Card Balance never becomes negative after any sequence of concurrent transactions.

---

### Requirement 6: Virtual Threads and Performance

**User Story:** As a platform engineer, I want the Authorizer to use Virtual Threads from Java 25, so that the server supports high throughput of concurrent requests without exhausting carrier threads.

#### Acceptance Criteria

1. THE Authorizer SHALL enable Virtual Threads via `spring.threads.virtual.enabled: true` in `application.yml`, so that the web server dispatcher uses a VirtualThread per request.
2. THE Authorizer SHALL use Virtual Threads for the processing of all REST requests and all database I/O operations performed during a request lifecycle.
3. WHILE the Authorizer is under load of at least 100 concurrent requests, THE Authorizer SHALL process all requests without returning thread exhaustion errors, with a maximum response time of 2000 ms per individual request.
4. IF the Authorizer detects that a Virtual Thread remained pinned on a carrier thread for more than 500 ms during an I/O operation, THEN THE Authorizer SHALL log a warning event identifying the operation responsible for the pinning.

---

### Requirement 7: Database Selection and Configuration

**User Story:** As a developer, I want to choose and configure a single database from the provided docker-compose, so that the environment is reproducible and the technical decision is documented.

#### Acceptance Criteria

1. THE Authorizer SHALL use exactly one of the databases declared in `docker-compose.yml` (MySQL 5.7 or MongoDB 4.2).
2. IF the selected database is MySQL, THEN THE Authorizer SHALL use JPA/Hibernate with ACID transaction support and OptimisticLocking via an integer version field incremented on each Card update.
3. IF the selected database is MongoDB, THEN THE Authorizer SHALL use multi-document transactions with an integer version field on the Card document, incremented on each update, for optimistic concurrency control.
4. WHEN a version conflict is detected by the database during Balance update, THE Authorizer SHALL abort the current operation, discard any partial changes, and return control to the retry mechanism in the service layer.
5. THE Authorizer SHALL comment out in `docker-compose.yml` the service definition of the unused database, keeping the definition of the selected database intact.
6. THE Authorizer SHALL document in `README.md` the justification for the database choice, including: (a) decision criteria used, (b) comparison between optimistic and pessimistic locking with rationale for the chosen approach, and (c) implications of the choice for consistency under concurrent load.

---

### Requirement 8: Unit Tests and Coverage

**User Story:** As a quality engineer, I want the Authorizer to have unit tests with high coverage, so that regressions are detected before deployment.

#### Acceptance Criteria

1. THE Authorizer SHALL have unit tests written with JUnit 5 and Mockito covering the service, controller, and authorization rule components.
2. THE Authorizer SHALL achieve at least 80% line coverage and branch coverage as measured by JaCoCo on the business packages (`controller`, `service`, `repository`).
3. WHEN at least 10 simultaneous threads fire transactions for the same Card with balance equal to the value of each transaction, THE Authorizer SHALL ensure that the final Card Balance equals the initial Balance minus the sum of all approved transactions (without duplication or loss of debit).
4. WHEN a POST `/transacoes` request is submitted in a scenario that triggers each AuthorizationRule (`CARD_NOT_FOUND`, `INVALID_PASSWORD`, `INSUFFICIENT_BALANCE`), THE Authorizer SHALL return HTTP 422 with body containing the corresponding rule code.
5. THE Authorizer SHALL generate a JaCoCo coverage report during the Maven `verify` phase.

---

### Requirement 9: Load Tests with k6

**User Story:** As a performance engineer, I want k6 scripts that exercise the three endpoints under load, so that bottlenecks and concurrency failures are identified before production.

#### Acceptance Criteria

1. WHEN the load test suite is started, THE Authorizer SHALL provide a k6 script that executes card creation in parallel with exactly 10 Virtual Users (VUs) for a duration of 30 seconds, where each VU performs sequential requests without a fixed interval between iterations.
2. WHEN the load test suite is started, THE Authorizer SHALL provide a k6 script that executes balance queries in parallel with exactly 10 VUs for a duration of 30 seconds, where each VU performs sequential requests without a fixed interval between iterations.
3. WHEN the load test suite is started, THE Authorizer SHALL provide a k6 script that executes concurrent transactions for the same card with exactly 20 VUs for a duration of 30 seconds, where all VUs use the same pre-created card identifier.
4. IF the resulting balance of any transaction processed during the concurrent transactions script is less than 0.00, THEN THE Authorizer SHALL record a check failure in the k6 report for that iteration, so that the total number of negative balance check failures equals zero at the end of execution.
5. WHEN the k6 scripts are executed against the locally running application, THE Authorizer SHALL maintain an error rate below 1% for card creation and balance query scenarios, where error rate is calculated as the number of responses with unexpected status divided by the total requests in the 30-second period.
6. WHEN the load test suite is completed, THE Authorizer SHALL display in the k6 summary the request duration metrics (minimum, median, 95th percentile, and maximum in milliseconds) and error rate for each script individually.

---

### Requirement 10: Project Setup and Docker Build Pipeline

**User Story:** As a developer, I want the project scaffolding, Docker image generation, and GitHub Actions workflow to follow the structure of rommanel-pix-integrator, so that the build and deployment pipeline is reproducible and consistent.

#### Acceptance Criteria

1. THE Authorizer SHALL use Spring Boot 4.x (as used in rommanel-pix-integrator) with `java.version` set to `25` in `pom.xml`.
2. THE Authorizer SHALL include a `Dockerfile` based on `eclipse-temurin:25-jdk`, accepting `ARTIFACT_NAME` and `IMAGE_VERSION` build args, writing a `version.properties` file, and delegating startup to `entrypoint.sh`.
3. THE Authorizer SHALL include an `entrypoint.sh` script that sources `version.properties`, logs the artifact name and image version, and executes the application JAR.
4. THE Authorizer SHALL include a `docker.properties` file declaring `IMAGE_NAME`, `MAJOR_VERSION`, `MINOR_VERSION`, and `ARTIFACT_NAME`.
5. THE Authorizer SHALL include a `.github/workflows/docker.yml` GitHub Actions workflow that: builds on push to `main`, sets up JDK 25 with Temurin distribution, builds the Maven artifact with `-DskipTests`, reads `docker.properties` via `madhead/read-java-properties`, generates a sequential build number via `einaregilsson/build-number`, and pushes the Docker image to Docker Hub with versioned and `latest` tags.
6. THE Authorizer SHALL include a `java.security` file in the project root for JVM security configuration, as required by the Dockerfile `ADD` instruction.

---

### Requirement 11: Logging Standards

**User Story:** As a developer maintaining the system, I want structured and consistent log messages following the project conventions, so that operations can be monitored and debugged efficiently.

#### Acceptance Criteria

1. THE Authorizer SHALL use `@Slf4j` (Lombok) annotation for logger injection in all service and controller classes.
2. THE Authorizer SHALL follow the log message format `ClassName.methodName - Description - key: {value}` for all log statements, consistent with the pattern established in `liv-customer-register`.
3. THE Authorizer SHALL log at `INFO` level: method entry with input parameters, successful operation completion with output summary, and each AuthorizationRule evaluation result.
4. THE Authorizer SHALL log at `DEBUG` level: intermediate processing steps, retry attempts for OptimisticLocking conflicts, and internal state transitions.
5. THE Authorizer SHALL log at `ERROR` level: unexpected exceptions with full stack trace, including class name, method name, and the triggering input that caused the failure.
6. THE Authorizer SHALL log a WARN-level message whenever a Virtual Thread pinning event exceeding 500 ms is detected, including the operation name and thread identifier.

---

### Requirement 12: Project Structure and Code Quality

**User Story:** As a developer who will maintain the system, I want the code to follow a clear layered architecture and consistent conventions, so that it is easy to understand, test, and evolve.

#### Acceptance Criteria

1. THE Authorizer SHALL organize code in the `controller`, `service`, and `repository` layers, where `controller` classes depend only on `service` classes, and `service` classes depend only on `repository` classes, with any direct dependency from `controller` to `repository` or from `repository` to `service` or `controller` being prohibited.
2. THE Authorizer SHALL use Java Records for all request and response DTOs, including `CreateCardRequest`, `CreateCardResponse`, `TransactionRequest`, and `TransactionResponse`.
3. THE Authorizer SHALL represent TransactionError types as an `enum` or `sealed interface` with the English values `CARD_NOT_FOUND`, `INVALID_PASSWORD`, `INSUFFICIENT_BALANCE`, and `INVALID_AMOUNT`.
4. THE Authorizer SHALL configure the JaCoCo Maven Plugin for automatic execution during the `verify` phase, with a minimum threshold of 80% line coverage, causing the build to fail if the threshold is not met.
5. THE Authorizer SHALL document in `README.md` the following sections: (a) design assumptions adopted, (b) justified architecture decisions, (c) execution instructions via docker-compose and Maven, and (d) how to run tests and verify the JaCoCo coverage report.
---

### Requirement 13: No-If Coding Pattern (Optional Challenge)

**User Story:** As a code reviewer, I want the authorization logic to be implemented without any `if` statement in the business flow, so that the design demonstrates polymorphism and the Open/Closed Principle in practice.

#### Acceptance Criteria

1. THE Authorizer SHALL implement the transaction authorization flow without any `if`, `else`, or ternary operator (`?:`) in the `TransactionServiceImpl.authorize()` method and in all `AuthorizationRule` implementations — each rule SHALL throw an exception or return silently, with no conditional branching via `if`.
2. THE Authorizer SHALL implement each `AuthorizationRule` as a distinct `@Component` bean that encapsulates a single authorization condition, so that adding a new rule requires only creating a new class, with no modification to `TransactionServiceImpl`.
3. THE Authorizer SHALL document in `README.md` the no-if design pattern used, explaining how the `List<AuthorizationRule>` chain achieves conditional behavior through polymorphism instead of explicit branching.

---

### Requirement 14: Multi-Instance Concurrency Safety (Optional Challenge)

**User Story:** As a system architect, I want the Authorizer to guarantee correct balance debit even when two transactions for the same card are processed simultaneously by **different application instances** sharing the same database, so that horizontal scaling does not introduce race conditions.

#### Acceptance Criteria

1. WHEN two POST `/transacoes` requests for the same `numeroCartao` are received by **two different running instances** of the Authorizer simultaneously, THE Authorizer SHALL guarantee that at most one transaction is approved — the database-level version check (`UPDATE ... WHERE version = ?`) SHALL act as the arbitration mechanism across instances.
2. THE Authorizer SHALL rely exclusively on the database as the single source of truth for the Card version field, ensuring that optimistic locking conflict detection works correctly regardless of how many application instances are running.
3. THE Authorizer SHALL document in `README.md` why the chosen optimistic locking strategy is inherently safe across multiple instances, contrasting it with in-JVM approaches (e.g., `synchronized`, `ReentrantLock`) that would fail in a distributed deployment.
4. THE Authorizer SHALL NOT use any JVM-level synchronization primitive (`synchronized`, `ReentrantLock`, `AtomicReference`, etc.) as the primary concurrency guard for balance updates — the database version field is the sole concurrency mechanism.
