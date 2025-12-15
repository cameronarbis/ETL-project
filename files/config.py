"""
Configuration file for data generator
Modify these settings to customize behavior
"""

# HTTP Endpoint Configuration
ENDPOINT = None  # Set to 'http://localhost:8080/api/transactions' when Spring Boot is ready

# Generation Settings
INTERVAL_SECONDS = 1.0  # Time between transactions
TRANSACTION_COUNT = None  # None for infinite, or set a number like 1000

# Output Settings
WRITE_TO_FILE = True
SEND_VIA_HTTP = False  # Set to True when endpoint is ready
OUTPUT_FILENAME = 'transactions.jsonl'

# Data Variation Settings
ANOMALY_RATE = 0.05  # 5% of transactions will be anomalies
INTERNATIONAL_CURRENCY_RATE = 0.10  # 10% will use non-USD currency
FAILURE_RATE = 0.15  # 15% will have pending/failed status
