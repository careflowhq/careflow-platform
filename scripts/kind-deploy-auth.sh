#!/usr/bin/env bash
# CareFlow — load auth-service image and deploy to Kind (Sprint 3.2)
# Usage (from repo root): ./scripts/kind-deploy-auth.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLUSTER_NAME="careflow-local"
IMAGE="careflow/auth-service:0.0.1-SNAPSHOT"
K8S_APP="$ROOT/infra/kubernetes/apps/auth-service"

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
  echo "!! Cluster '$CLUSTER_NAME' not found. Run ./scripts/kind-up.sh first."
  exit 1
fi

kubectl config use-context "kind-$CLUSTER_NAME"

if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
  echo "==> Image not found locally — building..."
  (cd "$ROOT/infra/docker" && docker compose -f docker-compose.build.yml build auth-service)
fi

echo "==> Loading image into Kind cluster..."
kind load docker-image "$IMAGE" --name "$CLUSTER_NAME"

echo "==> Deploying auth-service..."
kubectl apply -k "$K8S_APP"

echo "==> Waiting for auth-service..."
kubectl wait -n careflow --for=condition=ready pod -l app.kubernetes.io/name=auth-service --timeout=240s

echo ""
echo "Done."
kubectl get pods -n careflow -l app.kubernetes.io/name=auth-service
