#!/bin/bash

BASE_URL="http://localhost:8081"

echo "========================================"
echo "Testing Ingress Service API"
echo "========================================"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "Test 1: Health Check"
response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/transactions/health)
status_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$status_code" -eq 200 ]; then
    echo -e "${GREEN}✓ Health check passed${NC}"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Health check failed (Status: $status_code)${NC}"
fi

echo ""
echo "Test 2: Actuator Health"
response=$(curl -s -w "\n%{http_code}" $BASE_URL/actuator/health)
status_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$status_code" -eq 200 ]; then
    echo -e "${GREEN}✓ Actuator health check passed${NC}"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Actuator health check failed (Status: $status_code)${NC}"
fi

echo ""
echo "Test 3: Send Single Transaction"
response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-550e8400-e29b-41d4-a716-446655440000",
    "user_id": "USER_TEST",
    "timestamp": "2025-01-01T10:30:00",
    "type": "purchase",
    "amount": 99.99,
    "currency": "USD",
    "merchant": "Test Store",
    "category": "electronics",
    "status": "completed",
    "payment_method": "credit_card",
    "location": {
      "city": "TestCity",
      "country": "US",
      "ip_address": "192.168.1.1"
    },
    "metadata": {
      "device": "test",
      "session_id": "test-session-123"
    }
  }')

status_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$status_code" -eq 202 ]; then
    echo -e "${GREEN}✓ Transaction accepted${NC}"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Transaction failed (Status: $status_code)${NC}"
    echo "$body"
fi

echo ""
echo "Test 4: Send Batch of Transactions"
response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/transactions/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "transaction_id": "batch-001",
      "user_id": "USER_BATCH_1",
      "timestamp": "2025-01-01T10:31:00",
      "type": "purchase",
      "amount": 50.00,
      "currency": "USD",
      "merchant": "Store A",
      "category": "groceries",
      "status": "completed",
      "payment_method": "debit_card",
      "location": {
        "city": "City1",
        "country": "US",
        "ip_address": "10.0.0.1"
      },
      "metadata": {
        "device": "mobile",
        "session_id": "sess-batch-1"
      }
    },
    {
      "transaction_id": "batch-002",
      "user_id": "USER_BATCH_2",
      "timestamp": "2025-01-01T10:32:00",
      "type": "purchase",
      "amount": 75.50,
      "currency": "USD",
      "merchant": "Store B",
      "category": "dining",
      "status": "completed",
      "payment_method": "credit_card",
      "location": {
        "city": "City2",
        "country": "US",
        "ip_address": "10.0.0.2"
      },
      "metadata": {
        "device": "web",
        "session_id": "sess-batch-2"
      }
    }
  ]')

status_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$status_code" -eq 202 ]; then
    echo -e "${GREEN}✓ Batch accepted${NC}"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
else
    echo -e "${RED}✗ Batch failed (Status: $status_code)${NC}"
    echo "$body"
fi

echo ""
echo "Test 5: Check Metrics"
echo "Transactions Received:"
curl -s $BASE_URL/actuator/metrics/transactions.received | python3 -c "import sys, json; print(json.load(sys.stdin)['measurements'][0]['value'])" 2>/dev/null || echo "N/A"

echo "Transactions Published:"
curl -s $BASE_URL/actuator/metrics/transactions.published | python3 -c "import sys, json; print(json.load(sys.stdin)['measurements'][0]['value'])" 2>/dev/null || echo "N/A"

echo ""
echo "========================================"
echo "Tests Complete!"
echo "========================================"
echo ""
echo "Check Kafka UI at http://localhost:8080 to see the messages"
echo "Topic: transactions"
