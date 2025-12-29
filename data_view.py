"""
Transaction Viewer - Utility to view and analyze generated data
"""

import json
import argparse
from collections import Counter
from datetime import datetime

def view_transactions(filename='transactions.jsonl', count=10, pretty=True):
    """View transactions from JSONL file"""
    print(f"\n📊 Viewing {count} transactions from {filename}\n")
    print("="*80)
    
    with open(filename, 'r') as f:
        for i, line in enumerate(f):
            if i >= count:
                break
            
            transaction = json.loads(line)
            
            if pretty:
                print(f"\n🔹 Transaction #{i+1}")
                print(f"   ID: {transaction['transaction_id']}")
                print(f"   Type: {transaction['type']}")
                print(f"   Amount: {transaction['currency']} {transaction['amount']}")
                print(f"   Merchant: {transaction['merchant']}")
                print(f"   Status: {transaction['status']}")
                print(f"   Time: {transaction['timestamp']}")
            else:
                print(json.dumps(transaction, indent=2))
            
    print("\n" + "="*80)

def analyze_transactions(filename='transactions.jsonl'):
    """Analyze patterns in generated transactions"""
    
    types = Counter()
    statuses = Counter()
    currencies = Counter()
    categories = Counter()
    payment_methods = Counter()
    
    total_amount = 0
    count = 0
    
    with open(filename, 'r') as f:
        for line in f:
            transaction = json.loads(line)
            count += 1
            
            types[transaction['type']] += 1
            statuses[transaction['status']] += 1
            currencies[transaction['currency']] += 1
            categories[transaction['category']] += 1
            payment_methods[transaction['payment_method']] += 1
            total_amount += transaction['amount']
    
    print(f"\n📈 Analysis of {count} transactions")
    print("="*80)
    
    print(f"\n💰 Total Transaction Volume: ${total_amount:,.2f}")
    print(f"📊 Average Transaction: ${total_amount/count:,.2f}")
    
    print("\n📑 Transaction Types:")
    for ttype, tcount in types.most_common():
        percentage = (tcount/count)*100
        print(f"   {ttype:15} {tcount:5} ({percentage:5.1f}%)")
    
    print("\n✅ Transaction Status:")
    for status, scount in statuses.most_common():
        percentage = (scount/count)*100
        print(f"   {status:15} {scount:5} ({percentage:5.1f}%)")
    
    print("\n💵 Currencies:")
    for currency, ccount in currencies.most_common():
        percentage = (ccount/count)*100
        print(f"   {currency:15} {ccount:5} ({percentage:5.1f}%)")
    
    print("\n🏪 Top Categories:")
    for category, catcount in categories.most_common(5):
        percentage = (catcount/count)*100
        print(f"   {category:15} {catcount:5} ({percentage:5.1f}%)")
    
    print("\n💳 Payment Methods:")
    for method, mcount in payment_methods.most_common():
        percentage = (mcount/count)*100
        print(f"   {method:15} {mcount:5} ({percentage:5.1f}%)")
    
    print("\n" + "="*80)

def tail_transactions(filename='transactions.jsonl', count=10):
    """View last N transactions"""
    
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    print(f"\n📊 Last {count} transactions from {filename}\n")
    print("="*80)
    
    for i, line in enumerate(lines[-count:]):
        transaction = json.loads(line)
        print(f"\n🔹 Transaction #{len(lines)-count+i+1}")
        print(f"   Type: {transaction['type']:10} | Amount: {transaction['currency']} {transaction['amount']:10.2f}")
        print(f"   Status: {transaction['status']:10} | Time: {transaction['timestamp']}")
    
    print("\n" + "="*80)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='View and analyze generated transactions')
    parser.add_argument('--file', default='transactions.jsonl', help='Transaction file to analyze')
    parser.add_argument('--view', type=int, metavar='N', help='View first N transactions')
    parser.add_argument('--tail', type=int, metavar='N', help='View last N transactions')
    parser.add_argument('--analyze', action='store_true', help='Analyze transaction patterns')
    parser.add_argument('--raw', action='store_true', help='Show raw JSON instead of pretty format')
    
    args = parser.parse_args()
    
    try:
        if args.view:
            view_transactions(args.file, args.view, pretty=not args.raw)
        elif args.tail:
            tail_transactions(args.file, args.tail)
        elif args.analyze:
            analyze_transactions(args.file)
        else:
            # Default: show last 5 and analyze
            tail_transactions(args.file, 5)
            analyze_transactions(args.file)
    except FileNotFoundError:
        print(f"❌ Error: File '{args.file}' not found")
        print(f"   Generate some data first with: python3 run_generator.py --count 100")
    except Exception as e:
        print(f"❌ Error: {e}")

