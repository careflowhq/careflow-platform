#!/usr/bin/env bash
# Renovar certificados Let's Encrypt (cron en VPS)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/infra/docker"

sudo certbot renew --quiet
docker compose -f "$COMPOSE_DIR/docker-compose.staging.yml" exec -T nginx nginx -s reload

echo "Certificados renovados y nginx recargado."
