#!/usr/bin/env bash
# Renovar certificados Let's Encrypt (cron en VPS, sin sudo)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/infra/docker"
WEBROOT="$COMPOSE_DIR/certbot/www"
CERT_DIR="$COMPOSE_DIR/certbot/conf"
PRIMARY_DOMAIN="${CAREFLOW_PRIMARY_DOMAIN:-app.careflowhq.org}"
LOG_PREFIX="[$(date -Iseconds)]"

log() { echo "$LOG_PREFIX $*"; }

fix_cert_permissions() {
  docker run --rm -v "$CERT_DIR:/etc/letsencrypt" alpine sh -c \
    "chmod -R a+rX /etc/letsencrypt/live /etc/letsencrypt/archive && \
     chmod a+r /etc/letsencrypt/archive/${PRIMARY_DOMAIN}/privkey1.pem 2>/dev/null || true"
}

mkdir -p "$WEBROOT" "$CERT_DIR"

cd "$COMPOSE_DIR"

log "Comprobando nginx..."
docker compose -f docker-compose.staging.yml up -d nginx

log "Renovando certificados (certbot)..."
if docker run --rm \
  -v "$CERT_DIR:/etc/letsencrypt" \
  -v "$WEBROOT:/var/www/certbot" \
  certbot/certbot renew --webroot -w /var/www/certbot; then
  log "Certbot renew OK"
else
  log "ERROR: certbot renew falló"
  exit 1
fi

log "Ajustando permisos de certs..."
fix_cert_permissions

log "Recargando nginx..."
docker compose -f docker-compose.staging.yml exec -T nginx nginx -s reload

log "Listo."
