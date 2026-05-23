#!/usr/bin/env bash
# CareFlow — deploy staging on VPS
# Run on server as deploy user: ./scripts/deploy-staging.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/infra/docker"
ENV_FILE="$COMPOSE_DIR/.env"

cd "$COMPOSE_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "==> Creating .env from .env.staging.example"
  cp .env.staging.example .env
  echo "!! Edit $ENV_FILE with strong secrets, then run this script again."
  exit 1
fi

if grep -q "change-me" "$ENV_FILE"; then
  echo "!! Update secrets in $ENV_FILE before deploying."
  exit 1
fi

echo "==> Building and starting CareFlow staging..."
docker compose -f docker-compose.staging.yml --env-file .env up -d --build

echo ""
echo "==> Status:"
docker compose -f docker-compose.staging.yml ps

echo ""
echo "Done. Open http://YOUR_SERVER_IP (port 80)"
echo "Logs: docker compose -f docker-compose.staging.yml logs -f"
