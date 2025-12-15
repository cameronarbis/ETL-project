# 🚀 Quick Start Guide - Data Generator

## Installation (One-time setup)

```bash
# Install dependencies
pip3 install -r requirements.txt
```

## Quick Commands

### Generate 100 transactions to file
```bash
python3 run_generator.py --count 100 --interval 0.1
```

### Run continuously (Ctrl+C to stop)
```bash
python3 run_generator.py
```

### View your data
```bash
python3 view_data.py --analyze
```

### View last 10 transactions
```bash
python3 view_data.py --tail 10
```

### When Spring Boot is ready - send to HTTP endpoint
```bash
python3 run_generator.py --endpoint http://localhost:8080/api/transactions --http
```

## File Structure

```
data-generator/
├── data_generator.py      # Main generator classes
├── run_generator.py       # CLI runner script
├── view_data.py          # Data viewer & analyzer
├── config.py             # Configuration settings
├── requirements.txt      # Python dependencies
├── README.md            # Full documentation
└── transactions.jsonl   # Generated data (created when you run)
```

## What's Next?

1. ✅ **Data Generator** - You're here!
2. 🔲 **Docker Compose** - Set up Kafka + PostgreSQL
3. 🔲 **Spring Boot Ingress** - REST API to receive data
4. 🔲 **ETL Pipeline** - Transform and load to database
5. 🔲 **Monitoring** - Observe your pipeline

## Tips

- Start with small batches (--count 100) to test
- Use --interval 0.1 for faster generation during testing
- Check transactions.jsonl to see raw data
- Use view_data.py to analyze patterns
- The data intentionally has inconsistencies (good for ETL practice!)

## Common Issues

**Module not found?**
```bash
pip3 install -r requirements.txt
```

**Permission denied?**
```bash
chmod +x run_generator.py
```

**Want to clear old data?**
```bash
rm transactions.jsonl data_generator.log
```

Ready to move on to setting up Kafka and PostgreSQL? Let me know! 🎉
