.PHONY: build-common build-ingress build-etl build run-ingress run-etl run help

help:
	@echo "Available targets:"
	@echo "  make build          - Build common module and both services"
	@echo "  make build-common   - Build common module only"
	@echo "  make build-ingress  - Build ingress service only"
	@echo "  make build-etl      - Build ETL pipeline only"
	@echo "  make run            - Run both services (sequential)"
	@echo "  make run-ingress    - Run ingress service only"
	@echo "  make run-etl        - Run ETL pipeline only"

build-common:
	cd spring-common && mvn clean install -DskipTests

build-ingress: build-common
	cd spring-ingress-service && mvn package -DskipTests

run-ingress:
	cd spring-ingress-service && mvn spring-boot:run

build-etl: build-common
	cd spring-etl-pipeline && mvn package -DskipTests

run-etl:
	cd spring-etl-pipeline && mvn spring-boot:run

build: build-common build-ingress build-etl

run: run-ingress run-etl
