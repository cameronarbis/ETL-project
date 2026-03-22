# Transaction Ingress Service

Spring Boot REST API that receives transactions and publishes them to Kafka.

## Features

- ✅ REST API endpoints for single and batch transaction ingress
- ✅ Automatic JSON validation
- ✅ Kafka producer with reliability settings (acks=all, idempotence)
- ✅ Async processing with CompletableFuture
- ✅ Metrics and monitoring with Micrometer
- ✅ Health checks via Spring Actuator
- ✅ Comprehensive error handling
- ✅ Structured logging

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker running with Kafka and PostgreSQL

## Quick Start

### 1. Build the Application

```bash
# Using Maven wrapper (recommended)
./mvnw clean package

# Or using system Maven
mvn clean package
```

### 2. Run the Application

```bash
# Using the run script (easiest)
./run.sh

# Or using Maven
./mvnw spring-boot:run

# Or run the JAR directly
java -jar target/ingress-service-1.0.0.jar
```

### 3. Verify It's Running

```bash
# Health check
curl http://localhost:8081/api/transactions/health

# Or use the test script
./test-api.sh
```

## API Endpoints

### POST /api/transactions

Receive a single transaction.

**Example:**
```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "user_id": "USER_1234",
    "timestamp": "2025-01-01T10:30:00",
    "type": "purchase",
    "amount": 127.99,
    "currency": "USD",
    "merchant": "Tech Store",
    "category": "electronics",
    "status": "completed",
    "payment_method": "credit_card",
    "location": {
      "city": "Austin",
      "country": "US",
      "ip_address": "192.168.1.1"
    },
    "metadata": {
      "device": "mobile",
      "session_id": "session-123"
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Transaction accepted and queued for processing",
  "data": {
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACCEPTED"
  },
  "timestamp": "2025-01-01T10:30:05"
}
```

### POST /api/transactions/batch

Receive multiple transactions at once.

### GET /api/transactions/health

Simple health check endpoint.

### GET /actuator/health

Detailed health information including Kafka connectivity.

### GET /actuator/metrics

View application metrics:
- `transactions.received` - Total transactions received
- `transactions.published` - Successfully published to Kafka
- `transactions.failed` - Failed to publish
- `transactions.publish.time` - Time to publish

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

server:
  port: 8081

kafka:
  topic:
    transactions: transactions
```

## Testing with Python Data Generator

Once the service is running, update your Python generator:

```bash
# On your Raspberry Pi
python3 run_generator.py \
  --endpoint http://<your-main-machine-ip>:8081/api/transactions \
  --http \
  --interval 1.0
```

## Monitoring

### View Metrics

```bash
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/metrics/transactions.received
```

### View Logs

```bash
tail -f logs/ingress-service.log
```

### Kafka UI

View published messages at http://localhost:8080

## Project Structure

```
src/main/java/com/dataplatform/ingress/
├── IngressServiceApplication.java
├── config/
│   └── KafkaConfig.java
├── controller/
│   └── TransactionController.java
├── model/
│   ├── Transaction.java
│   └── ApiResponse.java
└── service/
    ├── KafkaProducerService.java
    └── TransactionService.java
```

## Troubleshooting

### Connection to Kafka fails

```bash
docker ps | grep kafka
telnet localhost 9092
tail -f logs/ingress-service.log
```

### Port 8081 already in use

Change port in `application.yml`:
```yaml
server:
  port: 8082
```

## Next Steps

1. ✅ Spring Boot Ingress (You are here!)
2. 🔲 Connect Python generator
3. 🔲 Build ETL pipeline
4. 🔲 Transform and load to PostgreSQL
