# Implementation Plan: Mini Autorizador

## Overview

Implementação incremental de um autorizador de cartões de benefício com Spring Boot 4.1.x, Java 25 (Virtual Threads), MySQL 5.7, Optimistic Locking com retry automático, regras de autorização sem if-chain, testes unitários (JUnit 5 + Mockito), testes baseados em propriedades (jqwik), testes de carga (k6) e cobertura JaCoCo ≥ 80%.

---

## Tasks

- [ ] 1. Scaffolding do projeto Maven e arquivos de infraestrutura
  - [ ] 1.1 Criar `pom.xml` com Spring Boot 4.1.x parent, `java.version=25`, e todas as dependências: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, `mysql-connector-j` (runtime), `flyway-core`, `flyway-mysql`, `spring-retry`, `spring-aspects`, `lombok` (optional), `h2` (test), `spring-boot-starter-test` (test); adicionar `jacoco-maven-plugin 0.8.13` com goals `prepare-agent`, `prepare-agent-integration` e `report` na fase `verify`, mais goal `check` com threshold mínimo de 80% para `LINE` e `BRANCH` nos pacotes `controller`, `service`, `rules`; adicionar `maven-compiler-plugin` com `annotationProcessorPath` para Lombok; configurar `spring-boot-maven-plugin` excluindo Lombok
    - _Requirements: 10.1, 12.4, 8.5_
  - [ ] 1.2 Criar `MiniAutorizadorApplication.java` em `com.br.vr.miniautorizador` com `@SpringBootApplication` e `@EnableRetry`
    - _Requirements: 10.1, 6.1_
  - [ ] 1.3 Criar `src/main/resources/application.yml` com `spring.threads.virtual.enabled: true`, datasource MySQL (`${MYSQL_HOST}`, `${MYSQL_PORT}`, `${MYSQL_USER}`, `${MYSQL_PASSWORD}`), `jpa.hibernate.ddl-auto: validate`, `jpa.open-in-view: false`, `flyway.enabled: true`, actuator expondo `health` e `info`
    - _Requirements: 6.1, 7.1, 7.2_
  - [ ] 1.4 Criar `src/main/resources/application-local.yml` com H2 in-memory (`jdbc:h2:mem:miniautorizador`), `jpa.hibernate.ddl-auto: create-drop`, `flyway.enabled: false` para uso em testes locais
    - _Requirements: 7.1, 8.1_
  - [ ] 1.5 Criar ou atualizar `docker-compose.yml` comentando o serviço MongoDB e mantendo o serviço MySQL 5.7 intacto com as variáveis de ambiente e porta padrão
    - _Requirements: 7.5, 7.1_
  - [ ] 1.6 Criar `java.security` na raiz do projeto (arquivo de configuração de segurança da JVM exigido pelo `ADD` do Dockerfile)
    - _Requirements: 10.6_
  - [ ] 1.7 Criar `Dockerfile` baseado em `eclipse-temurin:25-jdk`, aceitando `ARTIFACT_NAME` e `IMAGE_VERSION` como build args, adicionando o JAR, copiando `java.security` para `${JAVA_HOME}/conf/security/`, criando `version.properties`, copiando e tornando executável `entrypoint.sh`, com `ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]`
    - _Requirements: 10.2_
  - [ ] 1.8 Criar `entrypoint.sh` que executa `source version.properties`, imprime o nome do artefato e a versão da imagem, e executa `java -jar mini-autorizador.jar`
    - _Requirements: 10.3_
  - [ ] 1.9 Criar `docker.properties` com `IMAGE_NAME=mini-autorizador`, `MAJOR_VERSION=1`, `MINOR_VERSION=0`, `ARTIFACT_NAME=mini-autorizador`
    - _Requirements: 10.4_
  - [ ] 1.10 Criar `.github/workflows/docker.yml` com: trigger em push na branch `main`, setup do JDK 25 Temurin (`actions/setup-java@v4`), `einaregilsson/build-number@v3`, `madhead/read-java-properties@latest` lendo `docker.properties`, `mvn -B package -DskipTests`, `docker/build-push-action@v5` com tags `{user}/mini-autorizador:{MAJOR}.{MINOR}.{BUILD}` e `latest`
    - _Requirements: 10.5_

- [ ] 2. Domínio, repositório e migração Flyway
  - [ ] 2.1 Criar `Card.java` em `domains/` com `@Entity @Table(name="cards")`, anotações Lombok (`@Builder`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`), implements `Serializable`, campos: `@Id @Column(name="card_number") String cardNumber`, `@Column(name="password_hash") String passwordHash`, `@Column(name="balance") BigDecimal balance`, `@Version @Column(name="version") Long version`
    - _Requirements: 7.2, 5.2, 12.1_
  - [ ] 2.2 Criar `CardRepository.java` em `repositories/` estendendo `JpaRepository<Card, String>`
    - _Requirements: 12.1, 7.2_
  - [ ] 2.3 Criar `src/main/resources/db/migration/V1__create_cards_table.sql` com a tabela `cards`: `card_number VARCHAR(19) NOT NULL PK`, `password_hash VARCHAR(255) NOT NULL`, `balance DECIMAL(19,2) NOT NULL DEFAULT 500.00`, `version BIGINT NOT NULL DEFAULT 0`, `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
    - _Requirements: 7.2, 2.3_

- [ ] 3. Segurança
  - [ ] 3.1 Criar `SecurityConfig.java` em `configs/` com `@EnableWebSecurity`, `SecurityFilterChain` stateless com HTTP Basic habilitado, `InMemoryUserDetailsManager` com usuário `username` / `password`, bean `BCryptPasswordEncoder`
    - _Requirements: 1.1, 1.2, 1.3_

- [ ] 4. DTOs (Java Records) e tipos de erro
  - [ ] 4.1 Criar `CreateCardRequest.java` em `records/requests/` como `record` com campos `@NotBlank String numeroCartao` e `@NotBlank String senha`
    - _Requirements: 12.2, 2.1, 2.4_
  - [ ] 4.2 Criar `CreateCardResponse.java` em `records/responses/` como `record` com campos `String numeroCartao` e `String senha`
    - _Requirements: 12.2, 2.1, 2.2_
  - [ ] 4.3 Criar `TransactionRequest.java` em `records/requests/` como `record` com campos `@NotBlank String numeroCartao`, `@NotBlank String senhaCartao`, `@NotNull @DecimalMin("0.01") BigDecimal valor`
    - _Requirements: 12.2, 4.1, 4.9_
  - [ ] 4.4 Criar `TransactionError.java` em `enums/` como `enum` com valores `CARD_NOT_FOUND`, `INVALID_PASSWORD`, `INSUFFICIENT_BALANCE`, `INVALID_AMOUNT`
    - _Requirements: 12.3, 4.2, 4.3, 4.4_
  - [ ] 4.5 Criar `CardAlreadyExistsException.java` em `exceptions/` estendendo `RuntimeException`, com campo `CreateCardResponse card` acessível via getter
    - _Requirements: 2.2_
  - [ ] 4.6 Criar `CardNotFoundException.java` em `exceptions/` estendendo `RuntimeException`
    - _Requirements: 3.2_
  - [ ] 4.7 Criar `AuthorizationException.java` em `exceptions/` estendendo `RuntimeException`, com campo `TransactionError error` acessível via getter
    - _Requirements: 4.2, 4.3, 4.4, 4.9_

- [ ] 5. Regras de autorização (no-if pattern)
  - [ ] 5.1 Criar `AuthorizationRule.java` em `rules/` como `@FunctionalInterface` com método `void evaluate(Card card, TransactionRequest request)`, com javadoc explicando que lança `AuthorizationException` em caso de falha e retorna silenciosamente em caso de sucesso
    - _Requirements: 4.5, 12.1_
  - [ ] 5.2 Criar `CardExistsRule.java` em `rules/` com `@Component @Order(1) @RequiredArgsConstructor @Slf4j`, implementando `AuthorizationRule`; lança `AuthorizationException(CARD_NOT_FOUND)` se `card == null`; loga INFO com o resultado da avaliação
    - _Requirements: 4.2, 4.5, 11.3_
  - [ ] 5.3 Criar `PasswordMatchRule.java` em `rules/` com `@Component @Order(2) @RequiredArgsConstructor @Slf4j`, injetando `BCryptPasswordEncoder`; usa `passwordEncoder.matches(request.senhaCartao(), card.getPasswordHash())`; lança `AuthorizationException(INVALID_PASSWORD)` se não bater; loga INFO com o resultado
    - _Requirements: 4.3, 4.5, 11.3_
  - [ ] 5.4 Criar `SufficientBalanceRule.java` em `rules/` com `@Component @Order(3) @RequiredArgsConstructor @Slf4j`; compara `card.getBalance().compareTo(request.valor()) < 0`; lança `AuthorizationException(INSUFFICIENT_BALANCE)` se saldo insuficiente; loga INFO com o resultado
    - _Requirements: 4.4, 4.5, 11.3_

- [ ] 6. Interfaces de serviço e implementações
  - [ ] 6.1 Criar `CardService.java` em `services/` como interface com métodos: `CreateCardResponse createCard(CreateCardRequest request)` e `BigDecimal getBalance(String cardNumber)`
    - _Requirements: 12.1_
  - [ ] 6.2 Criar `TransactionService.java` em `services/` como interface com método: `void authorize(TransactionRequest request)`
    - _Requirements: 12.1_
  - [ ] 6.3 Criar `CardServiceImpl.java` em `services/impl/` com `@Service @Slf4j @RequiredArgsConstructor`; injetar `CardRepository` e `BCryptPasswordEncoder`; no método `createCard`: verificar existência do cartão, lançar `CardAlreadyExistsException` com `CreateCardResponse` se duplicado, persistir novo `Card` com `balance = BigDecimal("500.00")` e senha hasheada com BCrypt, retornar `CreateCardResponse`; no método `getBalance`: buscar cartão por número, lançar `CardNotFoundException` se não encontrado, retornar `balance`; logar INFO na entrada/saída de cada método no formato `ClassName.methodName - Description - key: {value}`
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 11.1, 11.2, 11.3_
  - [ ] 6.4 Criar `TransactionServiceImpl.java` em `services/impl/` com `@Service @Slf4j @RequiredArgsConstructor @Transactional`; anotar o método `authorize` com `@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))`; injetar `CardRepository`, `List<AuthorizationRule>` (Spring injeta ordenada) e `BCryptPasswordEncoder`; no método: buscar o `Card` (pode ser null — `CardExistsRule` valida), iterar as regras com `rules.forEach(r -> r.evaluate(card, request))`, debitar o saldo (`card.setBalance(card.getBalance().subtract(request.valor()))`), salvar; adicionar método `@Recover` que lança `AuthorizationException(INSUFFICIENT_BALANCE)` após 3 falhas; logar DEBUG em cada tentativa de retry
    - _Requirements: 4.1, 4.5, 5.1, 5.2, 5.3, 11.1, 11.2, 11.4_

- [ ] 7. Checkpoint — compilação e contexto de segurança
  - Garantir que o projeto compila com `mvn compile`, que o contexto Spring carrega sem erros, e verificar se as dependências do `pom.xml` estão corretas antes de avançar para controllers e handlers.

- [ ] 8. Controllers
  - [ ] 8.1 Criar `CardController.java` em `controllers/` com `@RestController @RequestMapping("/cartoes") @RequiredArgsConstructor @Slf4j`; método `POST` mapeado para `createCard`: chama `cardService.createCard(request)`, retorna `ResponseEntity.status(201).body(response)` — a exceção `CardAlreadyExistsException` é tratada pelo handler; método `GET /{numeroCartao}`: chama `cardService.getBalance(numeroCartao)`, retorna `ResponseEntity.ok(balance)` — a exceção `CardNotFoundException` é tratada pelo handler; logar INFO na entrada/saída
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 11.1, 11.2, 12.1_
  - [ ] 8.2 Criar `TransactionController.java` em `controllers/` com `@RestController @RequestMapping("/transacoes") @RequiredArgsConstructor @Slf4j`; método `POST`: chama `transactionService.authorize(request)`, retorna `ResponseEntity.status(201).body("OK")` — exceções de autorização tratadas pelo handler; logar INFO na entrada/saída
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 11.1, 11.2, 12.1_

- [ ] 9. Exception Handler global
  - [ ] 9.1 Criar `RestExceptionHandler.java` em `exceptions/handler/` com `@ControllerAdvice`; implementar handlers: `CardAlreadyExistsException` → `ResponseEntity.status(422).body(exception.getCard())`, `CardNotFoundException` → `ResponseEntity.status(404).build()`, `AuthorizationException` → `ResponseEntity.status(422).body(exception.getError().name())`, `MethodArgumentNotValidException` → `ResponseEntity.badRequest().build()`, `HttpMessageNotReadableException` → `ResponseEntity.badRequest().build()`, `Exception` (fallback) → `ResponseEntity.internalServerError().body(ProblemDetail genérico)`
    - _Requirements: 2.2, 3.2, 4.2, 4.3, 4.4, 4.8, 4.9_

- [ ] 10. Checkpoint — testes de integração mínimos via H2
  - Garantir que todos os testes de contexto Spring passam com o profile `local` (H2), confirmar que `mvn test` não falha, e que a cadeia completa Controller → Service → Repository funciona antes de avançar para testes detalhados.

- [ ] 11. Testes unitários — serviços
  - [ ] 11.1 Criar `CardServiceImplTest.java` em `src/test/` com `@ExtendWith(MockitoExtension.class)`; testar: `createCard` sucesso (retorna 201 body correto, persiste com saldo 500), `createCard` duplicata (lança `CardAlreadyExistsException` sem persistir), `getBalance` encontrado (retorna saldo), `getBalance` não encontrado (lança `CardNotFoundException`)
    - _Requirements: 8.1, 8.2_
  - [ ] 11.2 Criar `TransactionServiceImplTest.java` com `@ExtendWith(MockitoExtension.class)`; testar: `authorize` sucesso (saldo debitado corretamente), `authorize` com `CARD_NOT_FOUND`, `INVALID_PASSWORD`, `INSUFFICIENT_BALANCE`, `INVALID_AMOUNT`, retry em `OptimisticLockingFailureException` (verificar que o método é chamado até 3 vezes), recover após 3 falhas (lança `AuthorizationException(INSUFFICIENT_BALANCE)`)
    - _Requirements: 8.1, 8.4, 5.3_

- [ ] 12. Testes unitários — regras de autorização
  - [ ] 12.1 Criar `CardExistsRuleTest.java`; testar: lança `AuthorizationException(CARD_NOT_FOUND)` quando `card == null`, não lança exceção quando `card != null`
    - _Requirements: 8.1, 8.4_
  - [ ] 12.2 Criar `PasswordMatchRuleTest.java`; testar: lança `AuthorizationException(INVALID_PASSWORD)` quando `BCryptPasswordEncoder.matches` retorna false, não lança quando retorna true
    - _Requirements: 8.1, 8.4_
  - [ ] 12.3 Criar `SufficientBalanceRuleTest.java`; testar: lança `AuthorizationException(INSUFFICIENT_BALANCE)` quando `balance < valor`, não lança quando `balance >= valor`
    - _Requirements: 8.1, 8.4_

- [ ] 13. Testes unitários — controllers e handler
  - [ ] 13.1 Criar `CardControllerTest.java` com `@WebMvcTest(CardController.class)` e `@MockBean CardService`; testar todos os mapeamentos HTTP: POST 201, POST 422 (duplicata), GET 200 com body, GET 404
    - _Requirements: 8.1, 8.2_
  - [ ] 13.2 Criar `TransactionControllerTest.java` com `@WebMvcTest(TransactionController.class)` e `@MockBean TransactionService`; testar: POST 201 "OK", POST 422 CARD_NOT_FOUND, POST 422 INVALID_PASSWORD, POST 422 INSUFFICIENT_BALANCE, POST 422 INVALID_AMOUNT, POST 400 malformed
    - _Requirements: 8.1, 8.2_
  - [ ] 13.3 Criar `RestExceptionHandlerTest.java` com `@WebMvcTest` ou teste unitário direto; verificar serialização correta do body para cada tipo de exceção: `CardAlreadyExistsException` → JSON com `numeroCartao`/`senha`, `CardNotFoundException` → body vazio 404, `AuthorizationException` → string com nome do enum, `MethodArgumentNotValidException` → 400, fallback → 500
    - _Requirements: 8.1, 8.2_

- [ ] 14. Teste de concorrência
  - [ ] 14.1 Criar `ConcurrencyTest.java` com `@SpringBootTest` (profile `local`, H2); usar `CountDownLatch` + 10 threads disparando a mesma transação de R$500,00; após todas as threads concluírem, verificar que `finalBalance >= 0` e que `finalBalance == 500 - (approved × 500)`; verificar que apenas 1 transação foi aprovada
    - _Requirements: 8.3, 5.4, 5.1_

- [ ] 15. Checkpoint — cobertura JaCoCo
  - Executar `mvn verify` e confirmar que o threshold de 80% line + branch coverage é atingido. Se o build falhar por cobertura insuficiente, adicionar testes complementares antes de avançar.

- [ ] 16. Testes baseados em propriedades (jqwik)
  - [ ] 16.1 Adicionar dependência `net.jqwik:jqwik:1.9.x` (test scope) ao `pom.xml`
    - _Requirements: 8.1_
  - [ ] 16.2 Criar `CardCreationPropertyTest.java` com `@ExtendWith(JqwikExtension.class)` (ou `@AddLifecycleHook`):
    - **Property 1: Card creation round-trip — balance invariant** — `@ForAll` número de cartão (16 dígitos, não duplicado) + senha não-blank → `createCard` → `getBalance` → assert `== 500.00`. Anotar com `// Feature: mini-autorizador, Property 1`. **Validates: Requirements 2.1, 2.3, 3.1**
    - **Property 2: Duplicate card rejection preserves existing state** — criar cartão → `@ForAll` nova senha → tentar re-criar → assert 422 + saldo original inalterado. Anotar com `// Feature: mini-autorizador, Property 2`. **Validates: Requirements 2.2**
    - **Property 3: Blank field rejection** — `@ForAll` strings em branco/whitespace para `numeroCartao` ou `senha` → assert HTTP 400, sem escrita no DB. Anotar com `// Feature: mini-autorizador, Property 3`. **Validates: Requirements 2.4**
    - _Requirements: 8.1, 2.1, 2.2, 2.3, 2.4_
  - [ ] 16.3 Criar `TransactionPropertyTest.java`:
    - **Property 4: Successful transaction reduces balance by exactly the transaction amount** — `@ForAll` `valor` em `(0, 500]` → `authorize` → assert `balance == 500 - valor`. Anotar com `// Feature: mini-autorizador, Property 4`. **Validates: Requirements 4.1**
    - **Property 5: Invalid amount rejection** — `@ForAll` `valor <= 0` → assert 422 INVALID_AMOUNT + saldo inalterado. Anotar com `// Feature: mini-autorizador, Property 5`. **Validates: Requirements 4.9**
    - **Property 6: Authorization rule precedence ordering** — `@ForAll` requests violando múltiplas regras → assert que o código de erro corresponde à primeira regra da cadeia. Anotar com `// Feature: mini-autorizador, Property 6`. **Validates: Requirements 4.5**
    - _Requirements: 8.1, 4.1, 4.5, 4.9_
  - [ ] 16.4 Criar `ConcurrencyPropertyTest.java`:
    - **Property 7: Balance never goes negative under concurrent transactions** — `@ForAll` conjuntos de transações concorrentes → assert `finalBalance >= 0`. Anotar com `// Feature: mini-autorizador, Property 7`. **Validates: Requirements 5.4, 3.4**
    - **Property 8: Concurrent transaction atomicity — conservation of value** — `@ForAll` N transações concorrentes de valor V → assert `finalBalance == 500 - (approved × V)`. Anotar com `// Feature: mini-autorizador, Property 8`. **Validates: Requirements 5.1, 8.3**
    - _Requirements: 8.1, 5.1, 5.4_

- [ ] 17. Testes de carga k6
  - [ ] 17.1 Criar `k6/create-card.js` com 10 VUs, duração 30s, POST `/cartoes` com números de cartão aleatórios de 16 dígitos, check de `response.status === 201 || response.status === 422`, threshold `http_req_failed < 0.01` (erro rate < 1%), exibir métricas de duração (min, med, p95, max) no summary
    - _Requirements: 9.1, 9.5, 9.6_
  - [ ] 17.2 Criar `k6/check-balance.js` com 10 VUs, duração 30s, GET `/cartoes/{id}` para um cartão pré-criado via `setup()`, check de `response.status === 200`, threshold `http_req_failed < 0.01`, exibir métricas no summary
    - _Requirements: 9.2, 9.5, 9.6_
  - [ ] 17.3 Criar `k6/concurrent-transactions.js` com 20 VUs, duração 30s, POST `/transacoes` todos usando o mesmo cartão pré-criado via `setup()`; check que `response.status === 201 || body === "INSUFFICIENT_BALANCE"`; ao final de cada iteração, fazer GET no cartão e verificar `balance >= 0`; exibir métricas de duração p95 e error rate no summary
    - _Requirements: 9.3, 9.4, 9.6_

- [ ] 18. README
  - [ ] 18.1 Escrever `README.md` cobrindo: (a) premissas de design adotadas (Virtual Threads, Optimistic Locking, no-if pattern, BCrypt), (b) decisões arquiteturais (justificativa da escolha MySQL, comparação Optimistic vs Pessimistic Locking com rationale, implicações para concorrência), (c) instruções de execução via `docker-compose up` + `mvn spring-boot:run -Dspring-boot.run.profiles=local` e via Docker, (d) como executar testes com `mvn test` e verificar o relatório JaCoCo em `target/site/jacoco/index.html`, (e) comandos k6 para cada script
    - _Requirements: 7.6, 12.5_

- [ ] 19. Desafios opcionais — no-if e multi-instância
  - [ ] 19.1 Auditar `TransactionServiceImpl.authorize()` e todas as implementações de `AuthorizationRule` para garantir ausência total de `if`, `else` e operador ternário (`?:`) no fluxo de autorização; substituir qualquer condicional remanescente por Optional, pattern matching, ou lançamento direto de exceção
    - _Requirements: 13.1, 13.2_
  - [ ] 19.2 Adicionar teste `NoIfPatternTest.java` que usa reflexão para varrer o bytecode/source das classes `TransactionServiceImpl`, `CardExistsRule`, `PasswordMatchRule` e `SufficientBalanceRule` e assert que nenhum método contém instrução `if` ou `?:`, garantindo que a constraint é verificável automaticamente
    - _Requirements: 13.1_
  - [ ] 19.3 Escrever seção "No-If Design Pattern" no `README.md` explicando como a `List<AuthorizationRule>` chain substitui branching condicional por polimorfismo, com exemplo de como adicionar uma nova regra sem alterar o `TransactionServiceImpl`
    - _Requirements: 13.3_
  - [ ] 19.4 Escrever seção "Multi-Instance Concurrency Safety" no `README.md` explicando por que o Optimistic Locking via `UPDATE ... WHERE version = ?` é seguro em múltiplas instâncias: (a) o banco é árbitro único da versão, (b) contraste com `synchronized`/`ReentrantLock` que falham em deploys distribuídos, (c) validação: rodar 2 instâncias locais apontando para o mesmo MySQL e executar `k6/concurrent-transactions.js`
    - _Requirements: 14.3, 14.4_
  - [ ] 19.5 Garantir que `TransactionServiceImpl` NÃO usa nenhum primitivo de sincronização JVM (`synchronized`, `ReentrantLock`, `AtomicReference`) — o campo `@Version` do banco é o único mecanismo de concorrência; adicionar comentário Javadoc no método `authorize()` explicando essa decisão
    - _Requirements: 14.4_

- [ ] 20. Checkpoint final — build completo
  - Executar `mvn verify` para garantir que todos os testes passam e o threshold JaCoCo é atingido. Confirmar que `mvn package -DskipTests` gera o JAR corretamente. Verificar que os desafios opcionais (tasks 19.x) estão implementados e documentados no README.

---

## Notes

- Tasks marcadas com `*` são opcionais e podem ser puladas para um MVP mais rápido
- Cada task referencia os requisitos específicos para rastreabilidade
- Os checkpoints nas tasks 7, 10, 15 e 19 garantem validação incremental
- O projeto usa `application-local.yml` com H2 para todos os testes (`@SpringBootTest`), sem depender do MySQL nos testes automatizados
- Os testes de propriedade (jqwik) dependem da task 16.1 (adição da dependência) antes de qualquer implementação de property test
- As tasks de k6 (17.x) não precisam do projeto compilado para serem escritas; os scripts podem ser criados em paralelo com os testes JUnit
- A task 18 (README) pode ser executada a qualquer momento após a task 1 estar completa

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4", "1.5", "1.6", "1.9"] },
    { "id": 2, "tasks": ["1.7", "1.8", "1.10", "2.1", "2.2", "2.3", "4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7"] },
    { "id": 3, "tasks": ["3.1", "5.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.4", "6.1", "6.2"] },
    { "id": 5, "tasks": ["6.3", "6.4"] },
    { "id": 6, "tasks": ["8.1", "8.2"] },
    { "id": 7, "tasks": ["9.1"] },
    { "id": 8, "tasks": ["11.1", "11.2", "12.1", "12.2", "12.3", "13.1", "13.2", "13.3"] },
    { "id": 9, "tasks": ["14.1", "16.1", "17.1", "17.2", "17.3", "18.1"] },
    { "id": 10, "tasks": ["16.2", "16.3", "16.4"] },
    { "id": 11, "tasks": ["19.1", "19.4", "19.5"] },
    { "id": 12, "tasks": ["19.2", "19.3", "20"] }
  ]
}
```
