#!/usr/bin/env bash
# CareFlow — arranque local (Linux/macOS)
# Uso: ./scripts/start-local.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

echo "==> CareFlow — arranque local"

echo "==> Docker (PostgreSQL + RabbitMQ)..."
docker compose -f "$ROOT/infra/docker/docker-compose.yml" up -d
sleep 3

LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

declare -A SERVICES=(
  ["api-gateway"]=8080
  ["auth-service"]=8081
  ["patient-service"]=8082
  ["clinic-service"]=8083
  ["followup-service"]=8084
  ["notification-service"]=8085
)

for name in api-gateway auth-service patient-service clinic-service followup-service notification-service; do
  port="${SERVICES[$name]}"
  dir="$ROOT/backend/$name"
  if [[ ! -d "$dir" ]]; then
    echo "WARN: no existe $dir"
    continue
  fi
  echo "==> Iniciando $name :$port (log: logs/$name.log)..."
  (cd "$dir" && nohup mvn -q spring-boot:run > "$LOG_DIR/$name.log" 2>&1 &)
  sleep 1
done

echo "==> Iniciando frontend :3000..."
(cd "$ROOT/frontend" && nohup npm run dev > "$LOG_DIR/frontend.log" 2>&1 &)

echo ""
echo "Listo. Abre http://localhost:3000"
echo "Logs en ./logs/"
echo "Detener: ./scripts/stop-local.sh"
