# Implementation Roadmap: Production-Ready ETL Pipeline

## Overview

Transform the learning-exercise ETL project into a production-ready system with proper observability, testing, data quality, and operational stability. Timeline: ~5 working days.

## Architecture Principles

1. **DRY (Don't Repeat Yourself)**: Eliminate Transaction model duplication
2. **Observability First**: Structured logs, metrics, health checks
3. **Data Quality**: Validation at system boundaries, error tracking
4. **Testability**: Integration tests, end-to-end verification
5. **Operational Readiness**: Graceful shutdown, configuration management, runbooks

## Detailed Implementation Plan

### Phase 1: Architecture & Refactoring (Day 1)

**Goal**: Create foundational structure for production

#### 1.1 Create spring-common module
```
spring-common/
├── pom.xml
├── src/main/java/com/dataplatform/common/
│   ├── model/
│   │   ├── Transaction.java (shared)
│   │   └── TransactionLocation.java
│   ├── config/
│   │   ├── KafkaConstants.java
│   │   └── ValidationConfig.java
│   ├── dto/
│   │   ├── ValidationResult.java
│   │   ├── AuditEvent.java
│   │   └── MetricsEvent.java
│   └── util/
│       ├── TransactionValidator.java
│       └── DateTimeUtil.java
└── src/test/java/...
```

#### 1.2 Update service dependencies
- spring-ingress-service → depends on spring-common
- spring-etl-pipeline → depends on spring-common
- Remove duplicate Transaction models
- Update package imports

#### 1.3 Database schema updates
```sql
-- Add audit table
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id SERIAL PRIMARY KEY,
    operation VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    user_id VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add data quality checks
CREATE TABLE IF NOT EXISTS data_quality_issues (
    issue_id SERIAL PRIMARY KEY,
    transaction_uuid UUID,
    issue_type VARCHAR(100),
    severity VARCHAR(20),
    message TEXT,
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Phase 2: Observability (Day 2)

**Goal**: Structured logging, metrics, health checks

#### 2.1 Structured Logging
- Configure logback-spring.xml for JSON output
- Add correlation IDs to all requests
- Log at INFO level for normal flow, DEBUG for details

Example:
```json
{
  "timestamp": "2026-10-18T22:15:30.123Z",
  "level": "INFO",
  "service": "etl-pipeline",
  "logger": "com.dataplatform.etl.service.TransactionConsumerService",
  "message": "Successfully processed transaction",
  "transaction_id": "uuid-here",
  "user_id": "USER_1234",
  "amount": 150.00,
  "duration_ms": 145
}
```

#### 2.2 Metrics via Micrometer
- Transaction counters (processed, failed, duplicates)
- Processing latency (histogram)
- Kafka consumer lag
- Database connection pool metrics

Exposed via `/actuator/prometheus` for Prometheus scraping

#### 2.3 Health Checks
- Kafka broker connectivity
- PostgreSQL database connectivity
- Consumer group status
- DLQ backlog

### Phase 3: Data Quality & Validation (Day 3)

**Goal**: Catch bad data early, track issues

#### 3.1 Validation Framework
```java
@Service
public class TransactionValidationService {
    public ValidationResult validate(Transaction tx) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Required fields
        if (tx.getTransactionId() == null) 
            errors.add(new ValidationError("TRANSACTION_ID_REQUIRED"));
        
        // Domain rules
        if (tx.getAmount() != null && tx.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(new ValidationError("INVALID_AMOUNT"));
        
        // Reference integrity
        if (!categoryExists(tx.getCategory()))
            errors.add(new ValidationError("INVALID_CATEGORY"));
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

#### 3.2 Error Routing
- **Valid data**: Proceed to database
- **Validation errors**: Log to data_quality_issues, route to DLQ
- **Database errors**: Log with full context, route to DLQ
- **DLQ contents**: Include original data + error details

#### 3.3 Audit Trail
```java
@Aspect
@Service
public class AuditAspect {
    @After("@annotation(Audited)")
    public void auditInsert(JoinPoint jp, Audited audited) {
        // Log the operation with before/after values
        AuditEvent event = new AuditEvent(
            operation = "INSERT",
            entityType = "TRANSACTION",
            userId = getCurrentUser(),
            newValues = serialize(jp.getArgs()[0]),
            createdAt = now()
        );
        auditRepository.save(event);
    }
}
```

### Phase 4: Testing & Verification (Day 3-4)

**Goal**: Automated testing, confidence in correctness

#### 4.1 Integration Tests
```java
@SpringBootTest
@TestcontainersTest
public class TransactionETLIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer();
    
    @Container
    static PostgreSQLContainer db = new PostgreSQLContainer();
    
    @Test
    void testFullPipelineFlow() {
        // 1. Send transaction via REST API
        Transaction tx = createTestTransaction();
        restTemplate.postForEntity("http://localhost:8081/api/transactions", tx);
        
        // 2. Verify appears in Kafka
        ConsumerRecord<String, Transaction> record = kafkaConsumer.poll(TIMEOUT);
        assertThat(record.value()).isEqualTo(tx);
        
        // 3. Verify ETL processes it
        kafkaConsumer.sendMessage(tx);
        
        // 4. Verify appears in database
        Transaction stored = transactionRepository.findById(tx.getId());
        assertThat(stored).isEqualTo(tx);
    }
}
```

#### 4.2 End-to-End Test Script
```bash
#!/bin/bash
set -e

# Start infrastructure
docker-compose up -d

# Wait for services ready
sleep 30

# Generate test data
python data-generator/run_generator.py --count 50

# Check ingress received them
curl http://localhost:8081/health

# Check ETL processed them
curl http://localhost:8082/actuator/health

# Verify database has data
docker exec postgres psql -U datauser -d transactions_db \
  -c "SELECT COUNT(*) FROM transactions;"

# Check metrics
curl http://localhost:8082/actuator/prometheus | grep etl_transactions

echo "✅ End-to-end pipeline verified"
```

### Phase 5: Hardening (Day 4-5)

**Goal**: Production-grade reliability

#### 5.1 Graceful Shutdown
```java
@Component
public class ShutdownHandler implements ApplicationListener<ContextClosedEvent> {
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("Shutting down gracefully...");
        // 1. Stop accepting new messages
        kafkaListenerContainer.stop();
        // 2. Wait for in-flight processing to complete (with timeout)
        kafkaListenerContainer.getContainerProperties()
            .setShutdownTimeout(30000); // 30 seconds
        // 3. Close database connections
        dataSource.getHikariPoolMXBean().softEvictConnections();
        logger.info("Shutdown complete");
    }
}
```

#### 5.2 Configuration Management
```yaml
# application.yml - shared defaults
spring:
  profiles:
    active: ${SPRING_PROFILE:local}

---
# application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/transactions_db

---
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

#### 5.3 DLQ Replay
```java
@Service
public class DLQReplayService {
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void replayDLQ() {
        List<DLQMessage> messages = dlqRepository.findByStatus("PENDING");
        
        for (DLQMessage msg : messages) {
            try {
                Transaction tx = objectMapper.readValue(msg.getOriginalData(), Transaction.class);
                databaseService.insertTransaction(tx);
                dlqRepository.markAsReplayed(msg.getId());
                logger.info("Successfully replayed DLQ message {}", msg.getId());
            } catch (Exception e) {
                logger.error("Replay failed for DLQ message {}", msg.getId(), e);
                dlqRepository.incrementRetryCount(msg.getId());
            }
        }
    }
}
```

#### 5.4 Security Hardening
- ✅ No hardcoded credentials (use environment variables)
- ✅ Input validation on all API endpoints
- ✅ SQL injection prevention (use prepared statements - already done)
- ✅ Secure deserialization (trusted type lists in Kafka)
- ✅ HTTPS ready (add security config for production)

### Phase 6: Documentation (Day 5)

**Goal**: Operational clarity

#### 6.1 Architecture Guide
- System diagram showing all components
- Data flow explanation
- Error handling strategy
- Deployment architecture

#### 6.2 Operational Runbook
- Starting/stopping services
- Monitoring checklist
- Troubleshooting guide
- Scaling considerations
- DLQ handling procedures

#### 6.3 API Documentation
- OpenAPI/Swagger for ingress service
- Example requests/responses
- Error codes and meanings

## Success Metrics

After implementation, verify:

- [ ] All tests pass (unit + integration)
- [ ] Full data flow works: generator → ingress → kafka → etl → db
- [ ] Logs are JSON-structured and queryable
- [ ] Metrics exposed and collection-ready
- [ ] Failed records in DLQ with full error context
- [ ] Health checks pass for all services
- [ ] Configuration externalizable (no hardcoded values)
- [ ] Docker image buildable and runnable
- [ ] Documentation complete and clear
- [ ] CI/CD pipeline configured

## Estimated Effort

| Phase | Task | Duration |
|-------|------|----------|
| 1 | Refactoring & Commons Module | 8 hours |
| 2 | Observability Setup | 6 hours |
| 3 | Data Quality & Validation | 6 hours |
| 4 | Testing & Integration | 8 hours |
| 5 | Hardening & Reliability | 6 hours |
| 6 | Documentation | 4 hours |
| **Total** | | **38 hours** |

## Dependencies & Tools

- Maven 3.8+
- Java 21
- Docker & Docker Compose
- PostgreSQL 15+
- Kafka 7.5+
- Spring Boot 3.4.0

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Service duplication adds complexity | Clear module boundaries, shared commons |
| Kafka broker failures lose data | Manual offset commit, DLQ, replay logic |
| Database migrations fail silently | Flyway versioning, test migrations first |
| Metrics cardinality explosion | Label guidelines, careful metric design |
| Configuration inconsistency | Environment-based profiles, validation |
