#!/usr/bin/env bash
# CareFlow — deploy full stack to Kind: backend + frontend + ingress (Sprint 3.4)
# Usage (from repo root): ./scripts/kind-deploy-all.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLUSTER_NAME="careflow-local"
VERSION="0.0.1-SNAPSHOT"
K8S_APPS="$ROOT/infra/kubernetes/apps"

SERVICES=(
  "auth-service:careflow/auth-service:${VERSION}"
  "clinic-service:careflow/clinic-service:${VERSION}"
  "patient-service:careflow/patient-service:${VERSION}"
  "followup-service:careflow/followup-service:${VERSION}"
  "notification-service:careflow/notification-service:${VERSION}"
  "api-gateway:careflow/api-gateway:${VERSION}"
  "frontend:careflow/frontend:${VERSION}"
)

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
  echo "!! Cluster '$CLUSTER_NAME' not found. Run ./scripts/kind-up.sh first."
  exit 1
fi

kubectl config use-context "kind-$CLUSTER_NAME"

"$ROOT/scripts/kind-install-ingress.sh"

MISSING=()
for entry in "${SERVICES[@]}"; do
  image="${entry#*:}"
  if ! docker image inspect "$image" >/dev/null 2>&1; then
    MISSING+=("${entry%%:*}")
  fi
done

if ((${#MISSING[@]} > 0)); then
  echo "==> Building missing images: ${MISSING[*]}"
  for name in "${MISSING[@]}"; do
    (cd "$ROOT/infra/docker" && docker compose -f docker-compose.build.yml build "$name")
  done
fi

echo "==> Loading images into Kind cluster..."
for entry in "${SERVICES[@]}"; do
  image="${entry#*:}"
  echo "    $image"
  kind load docker-image "$image" --name "$CLUSTER_NAME"
done

echo "==> Deploying apps + ingress rules..."
kubectl apply -k "$K8S_APPS"

DEPLOY_ORDER=(
  auth-service
  clinic-service
  patient-service
  followup-service
  notification-service
  api-gateway
  frontend
)

for name in "${DEPLOY_ORDER[@]}"; do
  echo "==> Waiting for $name..."
  kubectl wait -n careflow --for=condition=ready pod \
    -l "app.kubernetes.io/name=$name" --timeout=360s
done

echo ""
echo "Done."
kubectl get pods -n careflow -l app.kubernetes.io/part-of=careflow-platform
echo ""
kubectl get ingress -n careflow
echo ""
echo "Open: http://localhost:8088"
