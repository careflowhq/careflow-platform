#!/usr/bin/env bash
# CareFlow - arranque local (Linux/macOS)
# Uso: ./scripts/start-local.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

wait_for_port() {
  local port=$1 name=$2 timeout=${3:-240}
  echo "    Esperando $name (puerto $port)..."
  local end=$((SECONDS + timeout))
  while (( SECONDS < end )); do
    if bash -c "echo >/dev/tcp/127.0.0.1/$port" 2>/dev/null; then
      echo "    $name listo (:$port)"
      return 0
    fi
    sleep 3
  done
  echo "WARN: $name no respondio en ${timeout}s"
  return 1
}

start_service() {
  local name=$1 port=$2
  local dir="$ROOT/backend/$name"
  if [[ ! -d "$dir" ]]; then
    echo "WARN: no existe $dir"
    return
  fi
  echo "==> Iniciando $name :$port (log: logs/$name.log)..."
  (cd "$dir" && nohup mvn -q spring-boot:run > "$LOG_DIR/$name.log" 2>&1 &)
  sleep 2
}

echo "==> CareFlow - arranque local"

echo "==> Docker (PostgreSQL + RabbitMQ)..."
docker compose -f "$ROOT/infra/docker/docker-compose.yml" up -d
sleep 5

LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

MICROSERVICES=(
  "auth-service:8081"
  "clinic-service:8083"
  "patient-service:8082"
  "followup-service:8084"
  "notification-service:8085"
)

echo "==> Iniciando microservicios..."
for entry in "${MICROSERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  start_service "$name" "$port"
done

echo "==> Esperando microservicios..."
for entry in "${MICROSERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  wait_for_port "$port" "$name" || true
done

start_service "api-gateway" "8080"
wait_for_port 8080 "api-gateway" || true

echo "==> Iniciando frontend :3000..."
(cd "$ROOT/frontend" && nohup npm run dev > "$LOG_DIR/frontend.log" 2>&1 &)

echo ""
echo "Listo. Abre http://localhost:3000"
echo "Logs en ./logs/"
echo "Detener: ./scripts/stop-local.sh"
