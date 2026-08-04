#!/usr/bin/env bash
# CareFlow — validate Docker image builds (Sprint 1)
# Usage (from repo root): ./scripts/validate-docker-build.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_DIR="$ROOT/infra/docker"

cd "$COMPOSE_DIR"

echo "==> Building all CareFlow application images..."
docker compose -f docker-compose.build.yml build

echo ""
echo "==> Built images:"
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep -E "^careflow/" || true

echo ""
echo "Done. All application images built successfully."
