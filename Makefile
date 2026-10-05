.PHONY: build-ingress run-ingress build-etl run-etl build run help

help:
	@echo "Available targets:"
	@echo "  make build          - Build both services"
	@echo "  make build-ingress  - Build ingress service only"
	@echo "  make build-etl      - Build ETL pipeline only"
	@echo "  make run            - Run both services (sequential)"
	@echo "  make run-ingress    - Run ingress service only"
	@echo "  make run-etl        - Run ETL pipeline only"

build-ingress:
	cd spring-ingress-service && mvn package -DskipTests

run-ingress:
	cd spring-ingress-service && mvn spring-boot:run

build-etl:
	cd spring-etl-pipeline && mvn package -DskipTests

run-etl:
	cd spring-etl-pipeline && mvn spring-boot:run

build: build-ingress build-etl

run: run-ingress run-etl
