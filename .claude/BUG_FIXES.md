# Critical Bug Fixes Required

## Overview

End-to-end testing discovered 4 critical bugs that prevent the pipeline from working. All must be fixed before production deployment.

---

## Bug #1: Kafka Deserialization Fails ✅ FIXED

### Issue
- Ingress publishes with `__TypeId__=com.dataplatform.ingress.model.Transaction`
- ETL tries to deserialize using that class name
- Class not in `TRUSTED_PACKAGES`, so deserialization throws `IllegalArgumentException`
- Every message fails; partition loops endlessly trying to reprocess

### Root Cause
Both services used separate `Transaction` models in different packages. Ingress publishes with its class name; ETL doesn't trust it.

### Solution Applied ✅
Refactored both services to use shared `com.dataplatform.common.model.Transaction` from spring-common module. Now:
- Same model, same package
- `__TypeId__` header matches what ETL trusts
- Deserialization succeeds

### Verification
After build:
```bash
# In ETL consumer logs, should see:
# "Received transaction {uuid}" (no deserialization errors)
# "Successfully processed transaction {uuid}" (no parser errors)
```

---

## Bug #2: Timestamp Parsing Fails ❌ NEEDS FIX

### Issue
Generator creates timestamps like: `2026-10-18T20:09:05.797323`
(No timezone indicator - not valid ISO 8601 for `Instant.parse()`)

ETL calls: `Instant.parse(transaction.getTimestamp())`
Result: `DateTimeParseException: Text '2026-10-18T20:09:05.797323' could not be parsed`

Every transaction fails to insert.

### Root Cause
Generator uses `datetime.now().isoformat()` which outputs naive local time without timezone.

### Fix Required

**Option A: Fix Generator (Recommended)**

File: `data-generator/data_generator.py`

```python
# Current (line 63):
'timestamp': datetime.now().isoformat(),

# Fix to:
'timestamp': datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z'),
```

Also add import at top:
```python
from datetime import datetime, timezone, timedelta
```

**Option B: Fix ETL Service**

File: `spring-etl-pipeline/src/main/java/com/dataplatform/etl/service/DatabaseService.java`

Change line 95 from:
```java
Timestamp.from(Instant.parse(transaction.getTimestamp()))
```

To:
```java
Timestamp.from(
    LocalDateTime.parse(transaction.getTimestamp())
        .atZone(ZoneId.systemDefault())
        .toInstant()
)
```

### Testing
```bash
# After fix, send a transaction:
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-1",
    "user_id": "user-1",
    "timestamp": "2026-10-18T20:15:30Z",
    "type": "purchase",
    "amount": 99.99,
    "currency": "USD",
    "merchant": "Store",
    "category": "groceries",
    "status": "completed",
    "payment_method": "credit_card"
  }'

# Should see in ETL logs: "Successfully processed transaction test-1"
```

---

## Bug #3: DLQ Publishing Fails Silently ❌ NEEDS FIX

### Issue
ETL has no Kafka producer configuration for DLQ.
Spring Boot falls back to default: `StringSerializer`

When DLQHandler tries to send a HashMap:
```java
kafkaTemplate.send(dlqTopic, transactionId, dlqMessage);  // HashMap being serialized
```

Serialization throws `ClassCastException`, caught silently, logged as CRITICAL.
Offset is committed anyway - record is **completely lost**.

### Root Cause
`DLQHandler` expects a configured producer that can serialize Maps to JSON.
ETL service has no explicit producer bean or YAML configuration.

### Fix Required

**Add Producer Configuration**

File: `spring-etl-pipeline/src/main/java/com/dataplatform/etl/config/DLQProducerConfig.java`

```java
package com.dataplatform.etl.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DLQProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean("dlqKafkaTemplate")
    public KafkaTemplate<String, Map<String, Object>> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    @Bean
    public ProducerFactory<String, Map<String, Object>> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

**Update DLQHandler to Use Producer**

File: `spring-etl-pipeline/src/main/java/com/dataplatform/etl/error/DLQHandler.java`

```java
// Change constructor:
public DLQHandler(
    @Qualifier("dlqKafkaTemplate")
    KafkaTemplate<String, Map<String, Object>> kafkaTemplate,
    ObjectMapper objectMapper,
    MeterRegistry meterRegistry) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
}
```

Also update the send call in `sendToDLQ` method to properly handle the template type.

### Testing
```bash
# Force a validation error that triggers DLQ:
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-dlq",
    "user_id": "user-dlq",
    "timestamp": "2026-10-18T20:15:30Z",
    "type": "invalid_type",  # This will fail validation
    "amount": 99.99,
    "currency": "USD",
    "merchant": "Store",
    "category": "groceries",
    "status": "completed",
    "payment_method": "credit_card"
  }'

# Check DLQ topic has the message:
./manage.sh topics  # Shows transactions-dlq
# View in Kafka UI: http://localhost:8080 > transactions-dlq
```

---

## Bug #4: Generator Config Broken ❌ NEEDS FIX

### Issue 1: Syntax Error in config.py
File: `data-generator/config.py` line 2
```python
ENDPOINT = http://localhost:8080/api/transactions  # Invalid Python syntax!
```

Causes `SyntaxError` when importing.

### Issue 2: Wrong Port
Even when fixed, uses port 8080 (Kafka UI) instead of 8081 (Ingress API).

### Issue 3: Broken Command Line
`run_generator.py` has `send_http = args.http or config.SEND_VIA_HTTP`
Which is always truthy, so `--no-http` doesn't work.

### Fix Required

**Fix config.py**

```python
# Line 2 - ADD QUOTES:
ENDPOINT = "http://localhost:8081/api/transactions"  # ← Fixed: quoted + port 8081

# Remaining config (verify):
INTERVAL = 0.5
WRITE_TO_FILE = True
SEND_VIA_HTTP = False  # Default to file, not HTTP

# Add this for test mode:
TEST_COUNT = 100  # How many to generate in test mode
```

**Fix run_generator.py**

Line with send_http assignment:
```python
# Current (broken):
send_http = args.http or config.SEND_VIA_HTTP

# Fixed:
send_http = args.http if args.http is not None else config.SEND_VIA_HTTP
```

Also check argparse setup:
```python
parser.add_argument('--http', action='store_true', help='Send via HTTP')
parser.add_argument('--endpoint', type=str, default=config.ENDPOINT, help='HTTP endpoint')
parser.add_argument('--interval', type=float, default=config.INTERVAL, help='Interval between transactions')
parser.add_argument('--count', type=int, default=config.TEST_COUNT, help='Number of transactions')
```

### Testing
```bash
cd data-generator

# Test file output (no HTTP):
python run_generator.py --count 10

# Test HTTP output:
python run_generator.py --count 10 --http --endpoint http://localhost:8081/api/transactions --interval 0.5

# Both should complete without SyntaxError or ConnectionError
```

---

## Additional Issues (Lower Priority)

### Data Correctness Issues
- **No UNIQUE on transaction_uuid**: Duplicate redeliveries create duplicate rows
  - Fix: Add `ALTER TABLE transactions ADD UNIQUE (transaction_uuid);`

- **Race condition in get_or_create functions**: Two threads creating same merchant/customer simultaneously
  - Fix: Use `INSERT ... ON CONFLICT ... RETURNING` instead of SELECT+INSERT

- **Unbounded locations growth**: IPs are random, so every transaction creates new location
  - Fix: Make generator use fixed IPs or change schema to be less sparse

- **Naive timestamps**: No timezone stored, breaks UTC handling
  - Fix: Use `TIMESTAMP WITH TIME ZONE` in PostgreSQL

### Database Fixes

```sql
-- Fix 1: Add unique constraint on transaction_uuid
ALTER TABLE transactions ADD CONSTRAINT uq_transaction_uuid UNIQUE (transaction_uuid);

-- Fix 2: Replace get_or_create functions with proper upserts
-- (See spring-etl-pipeline/src/main/resources/db/migration/V3__fix_upsert_functions.sql)

-- Fix 3: Change timestamp columns to include timezone
ALTER TABLE transactions ALTER COLUMN transaction_timestamp TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE audit_log ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;
```

---

## Fix Priority & Timeline

### Must Fix Before Running (Critical Path)
1. ✅ Bug #1: Shared commons (DONE)
2. ❌ Bug #2: Timestamp parsing (30 min)
3. ❌ Bug #3: DLQ producer config (1 hour)
4. ❌ Bug #4: Generator config (15 min)

**Total**: ~2 hours to get a working end-to-end pipeline

### Should Fix Before Production
- Data correctness issues (2 hours)
- Database schema improvements (1 hour)

**Total**: ~5 hours total to production-ready

---

## Verification Checklist

After each fix, verify:

```bash
# 1. Build services
make build

# 2. Start infrastructure
./manage.sh start && sleep 10

# 3. Run services (in background)
make run-ingress &
make run-etl &
sleep 5

# 4. Generate test data
cd data-generator
python run_generator.py --count 20 --http --interval 0.5

# 5. Check database
./manage.sh db
> SELECT COUNT(*) FROM transactions;
> SELECT COUNT(*) FROM dlq_messages;

# 6. Check logs for errors
# (Should see successful transaction processing, no errors)

# 7. Check health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

All checks should pass before proceeding to production deployment.
