#!/usr/bin/env bash
# Renovar certificados Let's Encrypt (cron en VPS, sin sudo)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/infra/docker"
WEBROOT="$COMPOSE_DIR/certbot/www"
CERT_DIR="$COMPOSE_DIR/certbot/conf"

docker run --rm \
  -v "$CERT_DIR:/etc/letsencrypt" \
  -v "$WEBROOT:/var/www/certbot" \
  certbot/certbot renew --quiet

docker compose -f "$COMPOSE_DIR/docker-compose.staging.yml" exec -T nginx nginx -s reload

echo "Certificados renovados y nginx recargado."
