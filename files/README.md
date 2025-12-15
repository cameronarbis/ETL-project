# Data Generator for ETL Pipeline Project

This Python data generator creates synthetic transaction data and log events for your ETL pipeline practice.

## Features

- **Transaction Generator**: Creates realistic financial transactions with variations
- **Flexible Output**: Can write to files (JSONL) and/or send via HTTP
- **Realistic Data**: Uses Faker library for authentic-looking data
- **Edge Cases**: Includes anomalies, failed transactions, international currencies
- **Data Inconsistencies**: Randomly includes optional fields to simulate real-world messiness

## Setup

### On Raspberry Pi / VM

```bash
# Install Python 3.7+
sudo apt-get update
sudo apt-get install python3 python3-pip

# Clone or copy these files to your machine
cd ~/data-generator

# Install dependencies
pip3 install -r requirements.txt
```

### On any system

```bash
pip install -r requirements.txt
```

## Usage

### Basic Usage - Write to File Only

```bash
python3 run_generator.py
```

This will generate transactions continuously and write them to `transactions.jsonl`.

### Generate Specific Count

```bash
python3 run_generator.py --count 1000
```

Generates exactly 1000 transactions then stops.

### Adjust Speed

```bash
python3 run_generator.py --interval 0.1
```

Generates a transaction every 0.1 seconds (10 per second).

### Send to HTTP Endpoint

```bash
python3 run_generator.py --endpoint http://localhost:8080/api/transactions --http
```

Sends each transaction to your Spring Boot ingress service.

### Disable File Writing

```bash
python3 run_generator.py --no-file --endpoint http://your-api:8080/api/transactions --http
```

Only sends via HTTP, doesn't write to file.

## Configuration

Edit `config.py` to change default settings:

- `ENDPOINT`: Your HTTP endpoint URL
- `INTERVAL_SECONDS`: Time between generations
- `TRANSACTION_COUNT`: How many to generate (None = infinite)
- `ANOMALY_RATE`: Percentage of anomalous transactions
- `WRITE_TO_FILE`: Whether to write to JSONL file by default
- `SEND_VIA_HTTP`: Whether to send via HTTP by default

## Sample Output

```json
{
  "transaction_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user_id": "USER_5432",
  "timestamp": "2024-12-14T10:30:45.123456",
  "type": "purchase",
  "amount": 127.99,
  "currency": "USD",
  "merchant": "Tech Solutions Inc",
  "category": "electronics",
  "status": "completed",
  "payment_method": "credit_card",
  "location": {
    "city": "Austin",
    "country": "US",
    "ip_address": "192.168.1.100"
  },
  "metadata": {
    "device": "mobile",
    "session_id": "sess_abc123"
  }
}
```

## Running as a Service (Optional)

To run this continuously on a Raspberry Pi or VM, you can create a systemd service:

```bash
sudo nano /etc/systemd/system/data-generator.service
```

Add:

```ini
[Unit]
Description=Transaction Data Generator
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/data-generator
ExecStart=/usr/bin/python3 /home/pi/data-generator/run_generator.py --interval 1.0
Restart=always

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable data-generator
sudo systemctl start data-generator
sudo systemctl status data-generator
```

## Next Steps

1. ✅ **Data Generator** (You are here!)
2. Set up Kafka and PostgreSQL with docker-compose
3. Build Spring Boot ingress service
4. Create ETL pipeline
5. Add monitoring and observability

## Troubleshooting

- **Import errors**: Make sure you've installed requirements: `pip3 install -r requirements.txt`
- **Permission errors**: Check file permissions in your directory
- **HTTP errors**: Verify your endpoint is running and accessible
- **View logs**: Check `data_generator.log` for detailed logging

## Data Schema Notes

The transaction schema intentionally differs from what you'll want in your database:
- Nested JSON (location, metadata) will need flattening
- Inconsistent optional fields require handling
- Mixed user_id formats (UUID vs USER_XXXX)
- Various currencies need normalization
- This messiness is intentional for ETL practice!
