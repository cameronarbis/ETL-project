"""
Data Generator - Emits fake transaction data
This script generates synthetic transaction data and can send it via HTTP or write to files
"""

import json
import logging
import random
import time
from datetime import datetime, timedelta
from faker import Faker
import requests
from typing import Dict, List, Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('data_generator.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)
fake = Faker()

class TransactionGenerator:
    """Generates fake transaction data"""
    
    def __init__(self, endpoint: Optional[str] = None):
        """
        Initialize the transaction generator
        
        Args:
            endpoint: Optional HTTP endpoint to send data to
        """
        self.endpoint = endpoint
        self.transaction_types = ['purchase', 'refund', 'transfer', 'withdrawal', 'deposit']
        self.merchants = [fake.company() for _ in range(50)]
        self.categories = ['groceries', 'electronics', 'dining', 'entertainment', 
                          'travel', 'utilities', 'healthcare', 'clothing', 'other']
        
    def generate_transaction(self) -> Dict:
        """Generate a single fake transaction"""
        
        transaction_type = random.choice(self.transaction_types)
        
        # Add some realistic variations and edge cases
        amount = round(random.uniform(1.99, 2500.00), 2)
        
        # Occasionally generate anomalies for testing
        if random.random() < 0.05:  # 5% chance of anomaly
            amount = round(random.uniform(5000, 25000), 2)
        
        # Negative amounts for refunds/withdrawals
        if transaction_type in ['refund', 'withdrawal']:
            amount = -abs(amount)
        
        transaction = {
            'transaction_id': fake.uuid4(),
            'user_id': fake.uuid4() if random.random() < 0.3 else f"USER_{random.randint(1000, 9999)}",
            'timestamp': datetime.now().isoformat(),
            'type': transaction_type,
            'amount': amount,
            'currency': random.choice(['USD', 'EUR', 'GBP', 'CAD']) if random.random() < 0.1 else 'USD',
            'merchant': random.choice(self.merchants),
            'category': random.choice(self.categories),
            'status': random.choice(['completed', 'pending', 'failed']) if random.random() < 0.15 else 'completed',
            'payment_method': random.choice(['credit_card', 'debit_card', 'bank_transfer', 'paypal', 'crypto']),
            'location': {
                'city': fake.city(),
                'country': fake.country_code(),
                'ip_address': fake.ipv4()
            },
            'metadata': {
                'device': random.choice(['mobile', 'web', 'pos']),
                'session_id': fake.uuid4()
            }
        }
        
        # Occasionally add optional fields (data inconsistency practice)
        if random.random() < 0.3:
            transaction['customer_email'] = fake.email()
        
        if random.random() < 0.2:
            transaction['notes'] = fake.sentence()
            
        return transaction
    
    def send_transaction(self, transaction: Dict) -> bool:
        """
        Send transaction to HTTP endpoint
        
        Args:
            transaction: Transaction data to send
            
        Returns:
            True if successful, False otherwise
        """
        if not self.endpoint:
            logger.warning("No endpoint configured, skipping HTTP send")
            return False
            
        try:
            response = requests.post(
                self.endpoint,
                json=transaction,
                headers={'Content-Type': 'application/json'},
                timeout=5
            )
            response.raise_for_status()
            logger.info(f"Sent transaction {transaction['transaction_id']} - Status: {response.status_code}")
            return True
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to send transaction: {e}")
            return False
    
    def write_to_file(self, transaction: Dict, filename: str = 'transactions.jsonl'):
        """
        Write transaction to JSONL file
        
        Args:
            transaction: Transaction data to write
            filename: Output file name
        """
        try:
            with open(filename, 'a') as f:
                f.write(json.dumps(transaction) + '\n')
            logger.debug(f"Wrote transaction {transaction['transaction_id']} to {filename}")
        except IOError as e:
            logger.error(f"Failed to write to file: {e}")
    
    def run(self, 
            interval: float = 1.0, 
            count: Optional[int] = None,
            write_file: bool = True,
            send_http: bool = True):
        """
        Run the data generator
        
        Args:
            interval: Time between transactions in seconds
            count: Number of transactions to generate (None for infinite)
            write_file: Whether to write to file
            send_http: Whether to send via HTTP
        """
        logger.info(f"Starting transaction generator (interval={interval}s, count={count or 'infinite'})")
        
        transactions_generated = 0
        
        try:
            while count is None or transactions_generated < count:
                transaction = self.generate_transaction()
                
                if write_file:
                    self.write_to_file(transaction)
                
                if send_http and self.endpoint:
                    self.send_transaction(transaction)
                
                transactions_generated += 1
                
                if transactions_generated % 100 == 0:
                    logger.info(f"Generated {transactions_generated} transactions")
                
                time.sleep(interval)
                
        except KeyboardInterrupt:
            logger.info(f"Stopped by user. Total transactions generated: {transactions_generated}")
        except Exception as e:
            logger.error(f"Error in generator: {e}")
            raise


class LogEventGenerator:
    """Generates fake system log events"""
    
    def __init__(self):
        self.log_levels = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
        self.services = ['auth-service', 'payment-service', 'user-service', 
                        'notification-service', 'analytics-service']
        self.event_types = ['api_request', 'database_query', 'cache_hit', 
                           'cache_miss', 'external_api_call', 'job_execution']
    
    def generate_log_event(self) -> Dict:
        """Generate a single fake log event"""
        
        level = random.choice(self.log_levels)
        service = random.choice(self.services)
        
        event = {
            'timestamp': datetime.now().isoformat(),
            'level': level,
            'service': service,
            'event_type': random.choice(self.event_types),
            'message': fake.sentence(),
            'trace_id': fake.uuid4(),
            'user_id': f"USER_{random.randint(1000, 9999)}" if random.random() < 0.7 else None,
            'duration_ms': random.randint(10, 5000),
            'status_code': self._get_status_code(level),
        }
        
        if level in ['ERROR', 'CRITICAL']:
            event['stack_trace'] = fake.text(max_nb_chars=200)
            event['error_code'] = f"ERR_{random.randint(1000, 9999)}"
        
        return event
    
    def _get_status_code(self, level: str) -> int:
        """Get appropriate HTTP status code based on log level"""
        if level in ['ERROR', 'CRITICAL']:
            return random.choice([400, 401, 403, 404, 500, 502, 503])
        elif level == 'WARNING':
            return random.choice([200, 201, 400, 429])
        else:
            return random.choice([200, 201, 204])


if __name__ == '__main__':
    # Example usage - configure these as needed
    
    # Option 1: Just write to file (no HTTP endpoint yet)
    generator = TransactionGenerator(endpoint=None)
    generator.run(interval=0.5, count=100, write_file=True, send_http=False)
    
    # Option 2: Send to HTTP endpoint (uncomment when your Spring Boot service is ready)
    # generator = TransactionGenerator(endpoint='http://localhost:8080/api/transactions')
    # generator.run(interval=1.0, write_file=True, send_http=True)
    
    # Option 3: Generate log events instead
    # log_generator = LogEventGenerator()
    # for i in range(50):
    #     event = log_generator.generate_log_event()
    #     print(json.dumps(event, indent=2))
    #     time.sleep(0.5)
