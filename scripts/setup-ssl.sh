#!/usr/bin/env bash
# CareFlow — Let's Encrypt SSL para staging (careflowhq.org)
# No requiere sudo: usa imagen certbot/certbot via Docker.
# Uso en VPS:
#   export CERTBOT_EMAIL="tu@email.com"
#   ./scripts/setup-ssl.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/infra/docker"
WEBROOT="$COMPOSE_DIR/certbot/www"
CERT_DIR="$COMPOSE_DIR/certbot/conf"
PRIMARY_DOMAIN="${CAREFLOW_PRIMARY_DOMAIN:-app.careflowhq.org}"
ROOT_DOMAIN="${CAREFLOW_ROOT_DOMAIN:-careflowhq.org}"
SERVER_IP="${CAREFLOW_SERVER_IP:-178.105.118.30}"

if [[ -z "${CERTBOT_EMAIL:-}" ]]; then
  echo "!! Define CERTBOT_EMAIL antes de ejecutar:"
  echo "   export CERTBOT_EMAIL=\"tu@email.com\""
  exit 1
fi

mkdir -p "$WEBROOT" "$CERT_DIR"

echo "==> Comprobando DNS (debe apuntar a $SERVER_IP)..."
for host in "$PRIMARY_DOMAIN" "$ROOT_DOMAIN" "www.$ROOT_DOMAIN"; do
  resolved="$(getent ahosts "$host" 2>/dev/null | awk '/STREAM/ {print $1; exit}' || true)"
  if [[ -z "$resolved" ]]; then
    echo "!! $host no resuelve. Configura DNS y espera propagación (5–60 min)."
    exit 1
  fi
  if [[ "$resolved" != "$SERVER_IP" ]]; then
    echo "!! $host apunta a $resolved (esperado $SERVER_IP)"
    exit 1
  fi
  echo "    OK $host -> $resolved"
done

cd "$COMPOSE_DIR"

echo "==> Reiniciando nginx (HTTP + webroot ACME)..."
docker compose -f docker-compose.staging.yml up -d nginx

echo "==> Solicitando certificado Let's Encrypt (Docker certbot)..."
docker run --rm \
  -v "$CERT_DIR:/etc/letsencrypt" \
  -v "$WEBROOT:/var/www/certbot" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d "$PRIMARY_DOMAIN" \
  -d "$ROOT_DOMAIN" \
  -d "www.$ROOT_DOMAIN" \
  --email "$CERTBOT_EMAIL" \
  --agree-tos \
  --non-interactive \
  --keep-until-expiring

echo "==> Activando configuración HTTPS..."
cp "$COMPOSE_DIR/nginx/default.ssl.conf" "$COMPOSE_DIR/nginx/default.conf"

docker compose -f docker-compose.staging.yml up -d nginx

echo ""
echo "Listo. Prueba:"
echo "  https://$PRIMARY_DOMAIN"
echo "  https://$ROOT_DOMAIN"
echo ""
echo "Renovación (cron usuario deploy):"
echo "  0 3 * * * $ROOT/scripts/renew-ssl.sh >> ~/careflow-ssl-renew.log 2>&1"
