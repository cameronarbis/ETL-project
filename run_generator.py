"""
Main runner for the data generator
Run this script to start generating data
"""

import argparse
from data_generator import TransactionGenerator, LogEventGenerator
import config

def main():
    parser = argparse.ArgumentParser(description='Run the data generator')
    parser.add_argument('--type', choices=['transaction', 'logs'], default='transaction',
                       help='Type of data to generate')
    parser.add_argument('--interval', type=float, default=config.INTERVAL_SECONDS,
                       help='Seconds between each data generation')
    parser.add_argument('--count', type=int, default=config.TRANSACTION_COUNT,
                       help='Number of records to generate (default: infinite)')
    parser.add_argument('--endpoint', type=str, default=config.ENDPOINT,
                       help='HTTP endpoint to send data to')
    parser.add_argument('--no-file', action='store_true',
                       help='Disable writing to file')
    parser.add_argument('--http', action='store_true',
                       help='Enable sending via HTTP')
    
    args = parser.parse_args()
    
    if args.type == 'transaction':
        generator = TransactionGenerator(endpoint=args.endpoint)
        generator.run(
            interval=args.interval,
            count=args.count,
            write_file=not args.no_file,
            send_http=args.http or config.SEND_VIA_HTTP
        )
    else:
        print("Log generator not yet implemented in main")
        # You can add log generator here if needed

if __name__ == '__main__':
    main()
