#!/bin/bash

set -e

echo "========================================"
echo "Building Ingress Service"
echo "========================================"

if [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi

echo ""
echo "========================================"
echo "Starting Ingress Service"
echo "========================================"
echo "Service will start on http://localhost:8081"
echo "Press Ctrl+C to stop"
echo ""

java -jar target/ingress-service-1.0.0.jar
