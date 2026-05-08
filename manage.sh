#!/bin/bash

# docker compose management script
# makes it easier to manage the data pipeline infrastructure

set -e

PROJECT_NAME="data-pipeline"
COMPOSE_FILE="docker-compose.yml"
DOCKER_COMPOSE_CMD=()

# colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# functions for colored output
print_header() {
  echo -e "${BLUE}======================${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}======================${NC}"
}

print_success() {
  echo -e "${GREEN} SUCCESS! $1${NC}"
}

print_error() {
  echo -e "${RED} ERROR! $1${NC}"
}

print_warning() {
  echo -e "${YELLOW} WARNING! $1${NC}"
}

print_info() {
  echo -e "${BLUE} INFO! $1${NC}"
}

compose() {
  "${DOCKER_COMPOSE_CMD[@]}" -p "$PROJECT_NAME" -f "$COMPOSE_FILE" "$@"
}

# check if docker-compose is installed
check_dependencies() {
  if command -v docker-compose &>/dev/null; then
    DOCKER_COMPOSE_CMD=(docker-compose)
  elif docker compose version &>/dev/null; then
    DOCKER_COMPOSE_CMD=(docker compose)
  else
    print_error "Docker Compose is not installed"
    echo "Please install Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
  fi

  if ! docker info &>/dev/null; then
    print_error "Docker is not running"
    echo "please start Docker Desktop or Docker Daemon"
    exit 1

  fi

}
#start all services
start_all() {
  print_header "Starting data pipeline infrastructure"
  compose up -d
  print_success "All services started"
  show_status
  show_urls
}

#stop all services
stop_all() {
  print_header "Stopping data pipeline infrastructure"
  compose down
  print_success "All services stopped"
}

# show service status
show_status() {
  print_header "Service status"
  compose ps
}

# show URLs
show_urls() {
  print_header "Service URLs"
  echo -e "${GREEN}Kafka UI:${NC}         http://localhost:8080"
  echo -e "${GREEN}pgAdmin:${NC}          http://localhost:5050"
  echo -e "${GREEN}PostgreSQL:${NC}       localhost:5432"
  echo -e "${GREEN}Kafka:${NC}            localhost:9092"
  echo ""
  print_info "pgAdmin credentails: admin@admin.com / admin123"
  print_info "PostgreSQL credentails: datauser / datapass123"
}

# check health of services
check_health() {
  print_header "Health check"

  # check Kafka
  if docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 &>/dev/null; then
    print_success "Kafka is healthy"
  else
    print_error "Kafka is not responding"
  fi

  # check PostgreSQL
  if docker exec postgres pg_isready -U datauser &>/dev/null; then
    print_success "PostgreSQL is healthy"
  else
    print_error "PostgreSQL is not responding"
  fi

  # check Zookeeper
  if docker exec zookeeper bash -c "nc -z localhost 2181" 2>/dev/null; then
    print_success "Zookeeper is healthy"
  else
    print_error " Zookeeper is not responding"
  fi
}

# list Kafka topics
list_topics() {
  print_header "Kafka topics"
  docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

}

# connect to PostgreSQL
connect_db() {
  print_header "Connecting to PostgreSQL"
  docker exec -it postgres psql -U datauser -d transactions_db
}

# show service logs
show_logs() {
  if [ -z "$1" ]; then
    compose logs -f --tail=100
  else
    compose logs -f --tail=100 "$1"
  fi
}

# Show help
show_help() {
  cat <<EOF
Data Pipeline Infrastructure Management

Usage: $0 [command]

Commands:
    start           Start all services
    stop            Stop all services
    restart         Restart all services
    status          Show service status
    logs [service]  Show logs (optionally for specific service)
    urls            Show service URLs and credentials
    health          Check health of all services
    
    Kafka commands:
    topics          List all Kafka topics
    
    Database commands:
    db              Connect to PostgreSQL
    
    help            Show this help message

Examples:
    $0 start              # Start all services
    $0 logs kafka         # Show Kafka logs
    $0 db                 # Connect to database

EOF
}

case "$1" in
start)
  check_dependencies
  start_all
  ;;
stop)
  check_dependencies
  stop_all
  ;;
restart)
  check_dependencies
  compose restart
  print_success "All services restarted"
  ;;
status)
  check_dependencies
  show_status
  ;;
logs)
  check_dependencies
  show_logs "$2"
  ;;
urls)
  show_urls
  ;;
health)
  check_dependencies
  check_health
  ;;
topics)
  check_dependencies
  list_topics
  ;;
db)
  check_dependencies
  connect_db
  ;;
help | --help | -h | "")
  show_help
  ;;
*)
  print_error "Unknown command: $1"
  echo ""
  show_help
  exit 1
  ;;
esac
