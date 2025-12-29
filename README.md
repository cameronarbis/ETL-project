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

