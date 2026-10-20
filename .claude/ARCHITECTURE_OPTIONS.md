# Production Architecture Options

Two competing designs are being evaluated for the production-ready ETL pipeline upgrade.

## Current System (Learning Phase)

**Data Flow:**
```
Python Generator (synthetic JSON) 
  → HTTP POST to Ingress (port 8081)
  → Kafka topic "transactions"
  → ETL Pipeline consumer (port 8082)
  → PostgreSQL (normalized schema)
```

**Database Schema:**
- `customers` (deduplicated by user_uuid/code)
- `merchants` (deduplicated by name)
- `categories` (pre-seeded: groceries, electronics, etc)
- `locations` (deduplicated by city+country+IP)
- `transactions` (FK references to all 4)
- `etl_errors` (failed records as JSONB)

**Known Issues:**
1. **Code duplication**: Transaction model defined separately in each service
2. **No observability**: Basic logging, no structured logs or metrics
3. **No testing**: Pipeline logic unverified
4. **Manual offset management**: Complex, error-prone error handling
5. **No schema versioning**: Breaking schema changes propagate to consumers
6. **No audit trail**: No record of what data was loaded when
7. **No data quality validation**: Weak input validation
8. **Generator disconnected**: Not integrated into the pipeline

## Option A: Shared Library with Commons Module

### Structure
```
project-root/
├── spring-common/              ← NEW: Shared across services
│   ├── pom.xml
│   └── src/main/java/com/dataplatform/common/
│       ├── model/Transaction.java (SINGLE SOURCE OF TRUTH)
│       ├── config/KafkaConstants.java
│       ├── validation/TransactionValidator.java
│       ├── audit/AuditEvent.java
│       └── metrics/MetricsRegistry.java
├── spring-ingress-service/     ← Depends on spring-common
│   ├── pom.xml (updated)
│   └── controller/TransactionController.java (uses common Transaction)
├── spring-etl-pipeline/        ← Depends on spring-common
│   ├── pom.xml (updated)
│   └── service/DatabaseService.java (uses common Transaction)
└── data-generator/
```

### Key Design Decisions
- **Single Transaction model**: Defined once in spring-common, used by both services
- **Shared validation**: TransactionValidator in commons validates before Kafka insert
- **Central audit**: AuditEvent and AuditAspect for tracking all inserts
- **Database migrations**: Flyway for schema versioning
- **Observability**: Micrometer metrics collected at service layer
- **Testing**: Integration tests with Testcontainers

### Advantages
✅ DRY principle: Transaction defined once
✅ Easy to maintain: Single location for changes
✅ Clear contracts: Commons defines what services can do
✅ Simpler dependency tree
✅ Better code reuse (validators, metrics, config constants)

### Disadvantages
❌ Adds complexity: New module to maintain
❌ Shared JAR: Both services must update when commons changes
❌ Compilation dependency: commons must compile before services

---

## Option B: Monolithic with Domain Layers

### Structure
```
project-root/
├── spring-ingress-service/
│   ├── src/main/java/com/dataplatform/ingress/
│   │   ├── api/              (API layer)
│   │   │   └── TransactionController.java
│   │   ├── service/          (Service layer)
│   │   │   └── TransactionService.java (business logic)
│   │   ├── repository/       (Repository layer)
│   │   ├── model/
│   │   │   └── IngressTransaction.java (extends BaseTransaction)
│   │   └── config/
│   │       └── ApplicationConfig.java
│   └── pom.xml
├── spring-etl-pipeline/
│   ├── src/main/java/com/dataplatform/etl/
│   │   ├── consumer/         (Message consumer layer)
│   │   ├── service/          (Service layer)
│   │   │   ├── TransactionService.java
│   │   │   └── EnrichmentService.java
│   │   ├── transform/        (Transformation logic)
│   │   │   └── TransactionTransformer.java
│   │   ├── repository/       (Data access)
│   │   ├── model/
│   │   │   └── ETLTransaction.java (extends BaseTransaction)
│   │   └── config/
│   │       └── ApplicationConfig.java
│   └── pom.xml
└── data-generator/
```

### Key Design Decisions
- **Base class**: BaseTransaction with common fields
- **Service-specific models**: IngressTransaction and ETLTransaction extend base
- **Composition over inheritance**: Services compose validators, transformers, etc
- **Database migrations**: Liquibase (more flexible than Flyway)
- **Event sourcing lite**: transaction_events table for audit trail
- **Kafka schema registry**: Protobuf/Avro for message versioning
- **AOP metrics**: Aspect-oriented programming for observability

### Advantages
✅ Services remain independent (no compile-time dependency)
✅ Flexible validation: Each service controls its rules
✅ Schema registry: Kafka messages versioned independently
✅ Event sourcing: Full audit trail naturally
✅ Easier testing: Services don't share code

### Disadvantages
❌ Some code duplication: Validators, transformers defined per-service
❌ Harder to sync: Changes to models must update both services
❌ More complex: Base class inheritance and composition patterns
❌ Schema registry adds operational complexity

---

## Comparison Matrix

| Criterion | Option A (Commons) | Option B (Monolithic) |
|-----------|-------------------|----------------------|
| Code duplication | Low | Medium |
| Maintenance | Easy (single source) | Moderate (some duplication) |
| Service independence | Low (shared JAR) | High (decoupled) |
| Testing complexity | Medium | Medium |
| Deployment | Both must update | Can deploy independently |
| Learning curve | Easier | More patterns to learn |
| Operational complexity | Low | Medium (schema registry) |
| Message versioning | Implicit (class changes) | Explicit (schema registry) |
| Schema changes | Coordinated | Per-service |

---

## Recommendation

**Option A (Shared Library)** is recommended because:

1. **Simplicity wins**: Eliminates code duplication immediately
2. **Easier to reason about**: Single Transaction model, single source of truth
3. **Lower operational burden**: No schema registry setup needed
4. **Faster to production**: Simpler architecture = faster implementation
5. **Appropriate for pipeline**: Ingress and ETL are tightly coupled by domain

This project is a **data pipeline**, not a loosely-coupled microservice architecture. Tight coupling is acceptable and even desirable here.

**Option B** is better for: Future service independence, when you want to scale ingress and ETL separately, or when handling different data models per service.

---

## Production Readiness Checklist

### Common to Both Options
- [ ] Structured JSON logging
- [ ] Micrometer metrics (processed, failed, latency)
- [ ] Health checks (Kafka, PostgreSQL, consumer lag)
- [ ] Graceful shutdown with timeout
- [ ] DLQ with replay logic
- [ ] Audit trail table + tracking
- [ ] Data quality validation layer
- [ ] Integration tests (Testcontainers)
- [ ] End-to-end test automation
- [ ] Security: no hardcoded credentials
- [ ] Configuration management (dev/staging/prod)
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Operational runbooks
- [ ] CI/CD ready

### Option A Specific
- [ ] spring-common module created
- [ ] Flyway migrations setup
- [ ] Both services depend on commons
- [ ] Shared Transaction model deployed

### Option B Specific
- [ ] Base Transaction class created
- [ ] Liquibase migrations setup
- [ ] Schema registry for Kafka
- [ ] Event sourcing table added

---

## Next Step

**AWAITING USER DECISION**: Review these options and choose A or B.

Once decided, Phase D (Implementation) proceeds with the chosen design.
