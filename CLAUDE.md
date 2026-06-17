# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Pipeline Overview

```
Python Generator → POST /api/transactions → spring-ingress-service → Kafka ("transactions" topic) → spring-etl-pipeline → PostgreSQL
```

The generator produces synthetic financial transaction JSON. The ingress service (port 8081) receives HTTP POSTs and publishes to Kafka without persisting. The ETL pipeline (port 8082) consumes from Kafka, transforms the flat JSON into a normalized relational schema, and loads it into PostgreSQL.

## Infrastructure

All infrastructure runs via Docker Compose. Use `manage.sh` to control it:

```bash
./manage.sh start          # Start Kafka, Zookeeper, PostgreSQL, Kafka UI, pgAdmin
./manage.sh stop
./manage.sh health         # Check Kafka, PostgreSQL, Zookeeper
./manage.sh logs kafka     # Tail logs for a specific service
./manage.sh topics         # List Kafka topics
./manage.sh db             # psql shell into transactions_db
./manage.sh urls           # Print service URLs and credentials
```

Service endpoints after `start`:
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8080`
- PostgreSQL: `localhost:5432` — credentials: `datauser / datapass123`
- pgAdmin: `http://localhost:5050` — credentials: `admin@admin.com / admin123`

## Building and Running the Spring Services

Both Spring services use Java 21 and Maven. Run from each service's directory:

```bash
# Build
mvn package -DskipTests

# Run
mvn spring-boot:run
# or
java -jar target/*.jar
```

- `spring-ingress-service/` → runs on port **8081**
- `spring-etl-pipeline/` → runs on port **8082**

## Running the Data Generator

The generator lives in `data-generator/` and uses Python 3.11+ with `uv` for dependency management.

```bash
cd data-generator

# Write transactions to transactions.jsonl (no HTTP)
python run_generator.py --count 100

# Send to ingress service via HTTP
python run_generator.py --http --endpoint http://localhost:8081/api/transactions --interval 1.0

# Continuous (infinite) generation to file
python run_generator.py
```

Configure defaults in `config.py` (endpoint, interval, anomaly rates). `data_generator.py` contains the `TransactionGenerator` and `LogEventGenerator` classes.

## Architecture Details

### Ingress Service (`spring-ingress-service/`)

- `TransactionController` — single `POST /api/transactions` endpoint + `/health`. Validates the body with Bean Validation and delegates to `KafkaProducerService`.
- `KafkaProducerService` — sends to the `transactions` Kafka topic using `transaction_id` as the message key. Publish is async (`CompletableFuture`); errors are logged but not surfaced to the caller.
- `KafkaConfig` — producer config; uses `JsonSerializer` for values.

### ETL Pipeline (`spring-etl-pipeline/`)

- `KafkaConsumerConfig` — manual offset commit (`AckMode.MANUAL`): offsets are only committed after a successful DB insert, preventing message loss on crash. Consumer group: `etl-pipeline-group`. Concurrency: 3 threads. Failed messages are routed to `transactions-dlq`.
- `Transaction.java` — mirrors the ingress service model; both are separate copies under their own packages (`com.dataplatform.ingress.model` vs `com.dataplatform.etl.model`).

### Database Schema (`init-scripts/01-init-schema.sql`)

The flat JSON transaction is normalized into five tables:

| Table | Purpose |
|---|---|
| `customers` | Deduped by `user_uuid` or `user_code` |
| `merchants` | Deduped by `merchant_name` |
| `categories` | Pre-seeded (groceries, electronics, dining, …) |
| `locations` | Deduped by `(city, country_code, ip_address)` |
| `transactions` | FK references to all four lookup tables |

Helper PL/pgSQL functions (`get_or_create_customer`, `get_or_create_merchant`, `get_or_create_location`) handle the upsert pattern for each lookup entity. An `etl_errors` table stores failed records as JSONB for inspection.

### Known Issues / In-Progress

- `KafkaConsumerConfig.java` has compile errors: `@EnableKakfa` (typo), `@Bean` methods placed outside the class body, and a factory method name typo (`kafkaListenerContainerFactor`). This file is a work in progress.
- The ETL `application.yml` connects to PostgreSQL as `postgres/postgres`, but Docker Compose creates the database with `datauser/datapass123` — these need to be reconciled before the ETL pipeline can connect.
- The `Transaction` model is duplicated between the two services; no shared library exists yet.
