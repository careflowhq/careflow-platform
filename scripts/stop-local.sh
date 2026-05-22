#!/usr/bin/env bash
# CareFlow — detener servicios locales (Linux/macOS)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Deteniendo contenedores Docker..."
docker compose -f "$ROOT/infra/docker/docker-compose.yml" down

echo "==> Deteniendo procesos Java (spring-boot) y Next.js en puertos conocidos..."
for port in 8080 8081 8082 8083 8084 8085 3000; do
  if command -v fuser >/dev/null 2>&1; then
    fuser -k "$port/tcp" 2>/dev/null || true
  elif command -v lsof >/dev/null 2>&1; then
    pid=$(lsof -ti ":$port" 2>/dev/null || true)
    [[ -n "$pid" ]] && kill $pid 2>/dev/null || true
  fi
done

echo "Listo."
