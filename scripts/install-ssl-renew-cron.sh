#!/usr/bin/env bash
# Instala cron de renovación SSL para el usuario actual (deploy en VPS).
# Idempotente: no duplica la entrada si ya existe.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RENEW_SCRIPT="$ROOT/scripts/renew-ssl.sh"
LOG_FILE="${CAREFLOW_SSL_RENEW_LOG:-$HOME/careflow-ssl-renew.log}"
CRON_MARKER="# careflow-ssl-renew"
CRON_LINE="0 3 * * * $RENEW_SCRIPT >> $LOG_FILE 2>&1 $CRON_MARKER"

if [[ ! -x "$RENEW_SCRIPT" ]]; then
  chmod +x "$RENEW_SCRIPT"
fi

existing="$(crontab -l 2>/dev/null || true)"
if echo "$existing" | grep -Fq "$CRON_MARKER"; then
  echo "Cron de renovación SSL ya instalado."
  crontab -l | grep -F "$CRON_MARKER" || true
  exit 0
fi

{
  echo "$existing" | grep -Fv "$CRON_MARKER" | grep -Fv "renew-ssl.sh" || true
  echo "$CRON_LINE"
} | sed '/^$/d' | crontab -

echo "Cron instalado (renovación diaria 03:00 UTC):"
crontab -l | grep -F "$CRON_MARKER"
echo "Log: $LOG_FILE"
