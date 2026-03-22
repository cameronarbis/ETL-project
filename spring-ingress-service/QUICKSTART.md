# Quick Start Guide

## Prerequisites Check

```bash
# Check Java version (need 17+)
java -version

# Check Maven
mvn -version

# Check Docker containers are running
docker ps | grep kafka
```

## Step 1: Build

```bash
cd spring-ingress-service

# Build with Maven wrapper
./mvnw clean package

# Or download wrapper first if needed
mvn -N wrapper:wrapper
./mvnw clean package
```

## Step 2: Run

```bash
# Option A: Using run script
./run.sh

# Option B: Using Maven
./mvnw spring-boot:run

# Option C: JAR directly
java -jar target/ingress-service-1.0.0.jar
```

Service starts on **http://localhost:8081**

## Step 3: Test

```bash
# Quick health check
curl http://localhost:8081/api/transactions/health

# Full test suite
./test-api.sh
```

## Step 4: Send Test Transaction

```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-123",
    "user_id": "USER_1234",
    "timestamp": "2025-01-01T10:30:00",
    "type": "purchase",
    "amount": 99.99,
    "currency": "USD",
    "merchant": "Test Store",
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

## Step 5: Verify in Kafka UI

1. Open http://localhost:8080
2. Click **Topics** → **transactions**
3. Click **Messages**
4. See your transaction!

## Step 6: Connect Python Generator

On Raspberry Pi:

```bash
# Get main machine IP first (on main machine)
hostname -I

# On Raspberry Pi
python3 run_generator.py \
  --endpoint http://<main-machine-ip>:8081/api/transactions \
  --http \
  --interval 1.0
```

## Troubleshooting

### "Java command not found"
```bash
# Mac
brew install openjdk@17

# Ubuntu
sudo apt-get install openjdk-17-jdk
```

### "Port 8081 already in use"
Edit `src/main/resources/application.yml` and change port

### "Cannot connect to Kafka"
```bash
docker ps | grep kafka
cd ../ && ./manage.sh start
```

## What's Next?

```
✅ Ingress service running
🔲 Build ETL pipeline
🔲 Transform and load to PostgreSQL
```

## Architecture

```
Raspberry Pi → HTTP POST → Spring Boot (8081) → Kafka (9092) → ETL → PostgreSQL (5432)
```
