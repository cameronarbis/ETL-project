# Getting Started with Production ETL Pipeline

A complete, production-ready transaction ETL system with proper observability, testing, and data quality.

## Quick Start (5 minutes)

### Prerequisites
- Docker & Docker Compose
- Java 21
- Maven 3.8+
- Python 3.11+ (for data generator)

### 1. Start Infrastructure
```bash
# Start Kafka, PostgreSQL, and other services
./manage.sh start

# Verify services are running
./manage.sh health
```

### 2. Build Services
```bash
# Build common library and both services
make build

# Or build individually:
make build-common
make build-ingress
make build-etl
```

### 3. Run Services
In separate terminals:
```bash
# Terminal 1: Ingress service
make run-ingress
# Runs on http://localhost:8081

# Terminal 2: ETL pipeline
make run-etl
# Runs on http://localhost:8082
```

### 4. Generate Test Data
```bash
cd data-generator

# Generate 100 transactions and send to ingress service
python run_generator.py --count 100 --http --endpoint http://localhost:8081/api/transactions --interval 0.5
```

### 5. Verify Data Loaded
```bash
# Check database
./manage.sh db

# Inside psql:
SELECT COUNT(*) FROM transactions;
SELECT * FROM transactions LIMIT 5;
```

## Service URLs & Credentials

After `./manage.sh start`:

| Service | URL | Credentials |
|---------|-----|-------------|
| Ingress API | http://localhost:8081 | N/A |
| Ingress Health | http://localhost:8081/actuator/health | N/A |
| ETL Metrics | http://localhost:8082/actuator/prometheus | N/A |
| ETL Health | http://localhost:8082/actuator/health | N/A |
| Kafka UI | http://localhost:8080 | N/A |
| pgAdmin | http://localhost:5050 | admin@admin.com / admin123 |
| PostgreSQL | localhost:5432 | datauser / datapass123 |

## Architecture Overview

```
Data Generator (Python)
    ↓ (HTTP POST /api/transactions)
Ingress Service (8081)
    ├─ Validates transaction
    ├─ Publishes to Kafka
    └─ Returns 202 ACCEPTED
    ↓ (Kafka topic: "transactions")
Kafka Cluster
    ↓
ETL Pipeline Consumer (8082)
    ├─ Consumes from Kafka
    ├─ Transforms flat JSON to normalized schema
    ├─ Validates data quality
    ├─ Inserts into PostgreSQL
    └─ Routes failures to DLQ
    ↓
PostgreSQL Database
    ├─ customers (deduplicated)
    ├─ merchants (deduplicated)
    ├─ categories (predefined)
    ├─ locations (deduplicated)
    ├─ transactions (main fact table)
    ├─ audit_log (full audit trail)
    ├─ data_quality_issues (validation errors)
    ├─ dlq_messages (failed messages for replay)
    └─ etl_errors (legacy error tracking)
```

## Key Features

### ✅ Code Quality
- **No duplication**: Single Transaction model in spring-common
- **Shared validators**: Consistent validation across services
- **Typed errors**: ValidationError and ValidationResult classes
- **Clear boundaries**: Service layers, domain models, configs

### ✅ Observability
- **Health checks**: `/actuator/health` on both services
- **Metrics**: Prometheus-compatible on `/actuator/prometheus`
- **Structured logging**: JSON logs with service context
- **Audit trail**: Full history in audit_log table

### ✅ Data Quality
- **Validation**: TransactionValidator checks all required fields
- **Type safety**: Enum-like validation for status, type, etc
- **Error tracking**: data_quality_issues table for investigation
- **DLQ replay**: Failed messages can be retried

### ✅ Reliability
- **Graceful shutdown**: Services finish in-flight work before stopping
- **Manual offset commit**: Kafka offsets only committed after DB insert succeeds
- **Schema versioning**: Flyway migrations with version tracking
- **Connection pooling**: HikariCP with production settings

## Configuration

### Environment Variables

```bash
# Database
export DB_HOST=localhost          # PostgreSQL host
export DB_PORT=5432              # PostgreSQL port
export DB_NAME=transactions_db    # Database name
export DB_USER=datauser           # Database user
export DB_PASSWORD=datapass123    # Database password

# Kafka
export KAFKA_BOOTSTRAP=localhost:9092  # Kafka bootstrap servers
export KAFKA_TOPIC=transactions        # Kafka topic name
export KAFKA_DLQ_TOPIC=transactions-dlq # Dead letter queue topic

# Services
export INGRESS_PORT=8081          # Ingress service port
export ETL_PORT=8082              # ETL service port
export SPRING_PROFILE=production   # Spring profile (local/staging/prod)
```

### Application Profiles

```bash
# Local development (default)
mvn spring-boot:run  # uses application-local.yml

# Staging
mvn spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=staging'

# Production
mvn spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=prod'
```

## Monitoring & Operations

### Check Service Health
```bash
# Ingress service
curl http://localhost:8081/actuator/health

# ETL service
curl http://localhost:8082/actuator/health
```

### View Metrics
```bash
# Prometheus format
curl http://localhost:8082/actuator/prometheus | grep etl_

# Count transactions processed
curl http://localhost:8082/actuator/prometheus | grep etl_transactions_processed
```

### Check Database
```bash
# List all transactions
./manage.sh db
> SELECT * FROM transactions LIMIT 10;

# Check failed messages
> SELECT COUNT(*) FROM dlq_messages WHERE status = 'PENDING';

# Check audit trail
> SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 10;

# Check data quality issues
> SELECT issue_type, COUNT(*) FROM data_quality_issues GROUP BY issue_type;
```

### View Logs
```bash
# Ingress service logs
make run-ingress 2>&1 | grep -i error

# ETL service logs
make run-etl 2>&1 | grep -i error

# Docker service logs
./manage.sh logs kafka
./manage.sh logs postgres
```

## Testing

### Unit Tests
```bash
# Run unit tests for spring-common
cd spring-common && mvn test

# Run unit tests for ingress
cd ../spring-ingress-service && mvn test

# Run unit tests for ETL
cd ../spring-etl-pipeline && mvn test
```

### Integration Tests
```bash
# Full pipeline integration test (requires Docker)
cd spring-etl-pipeline && mvn verify
```

### Manual End-to-End Test
```bash
# 1. Start everything
./manage.sh start
make build
make run-ingress &  # Background
make run-etl &      # Background

# 2. Wait for services to start
sleep 5

# 3. Send test transaction
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-123",
    "user_id": "user-456",
    "timestamp": "2026-10-18T22:15:30Z",
    "type": "purchase",
    "amount": 99.99,
    "currency": "USD",
    "merchant": "Test Store",
    "category": "groceries",
    "status": "completed",
    "payment_method": "credit_card"
  }'

# 4. Wait for processing
sleep 2

# 5. Verify in database
./manage.sh db
> SELECT * FROM transactions WHERE transaction_uuid = 'test-123';

# 6. Check metrics
curl http://localhost:8082/actuator/prometheus | grep etl_transactions_processed
```

## Troubleshooting

### Services won't start
- Check Docker: `docker ps`
- Check ports: `lsof -i :8081` and `lsof -i :8082`
- Check logs: `make run-ingress` (don't background it)

### No data in database
- Check Kafka UI: http://localhost:8080 (messages in topic?)
- Check logs for validation errors
- Check data_quality_issues table for what failed

### Slow processing
- Check consumer lag: `curl http://localhost:8082/actuator/prometheus | grep kafka_consumer_lag`
- Check database connection pool: `curl http://localhost:8082/actuator/prometheus | grep hikari`
- Check ETL service logs for slow operations

### DLQ backlog growing
- Check dlq_messages table: `SELECT COUNT(*) FROM dlq_messages WHERE status = 'PENDING';`
- Check error_message for what's failing: `SELECT DISTINCT error_type FROM dlq_messages;`
- Investigate and fix root cause
- Replay with manual intervention or DLQ replay job

## Production Deployment

### Pre-deployment Checklist
- [ ] All tests pass (`mvn test`)
- [ ] Build succeeds (`make build`)
- [ ] Docker image created
- [ ] Environment variables configured
- [ ] Database migrations verified
- [ ] Monitoring dashboards ready
- [ ] Alerting thresholds configured
- [ ] Runbooks written
- [ ] Team trained

### Deployment Steps
```bash
# 1. Build Docker images
docker build -t etl-pipeline/spring-common:1.0.0 ./spring-common
docker build -t etl-pipeline/ingress:1.0.0 ./spring-ingress-service
docker build -t etl-pipeline/etl:1.0.0 ./spring-etl-pipeline

# 2. Run migrations
docker run --rm \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://db:5432/transactions_db" \
  -e SPRING_DATASOURCE_USERNAME="datauser" \
  -e SPRING_DATASOURCE_PASSWORD="datapass123" \
  etl-pipeline/etl:1.0.0 \
  /flyway/flyway migrate

# 3. Start services with docker-compose
docker-compose -f docker-compose.prod.yml up -d

# 4. Verify health
curl http://service-ip:8081/actuator/health
curl http://service-ip:8082/actuator/health
```

## Next Steps

1. **Set up monitoring**: Prometheus + Grafana for metrics visualization
2. **Configure alerting**: PagerDuty / Slack for critical alerts
3. **Load testing**: Verify throughput under realistic load
4. **Backup strategy**: PostgreSQL backup schedule and retention
5. **Disaster recovery**: Plan and test recovery procedures
6. **Security review**: Credentials, network policies, compliance

## Support

- **Issues**: Check logs and metrics first
- **Questions**: Review ARCHITECTURE.md and operational runbooks
- **Contributing**: Follow commit message format and code style

---

**Last Updated**: 2026-10-18 | **Version**: 1.0.0 | **Status**: Production Ready
