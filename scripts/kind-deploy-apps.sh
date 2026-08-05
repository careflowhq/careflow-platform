#!/usr/bin/env bash
# CareFlow — load backend images and deploy all apps to Kind (Sprint 3.3)
# Usage (from repo root): ./scripts/kind-deploy-apps.sh

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
)

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
  echo "!! Cluster '$CLUSTER_NAME' not found. Run ./scripts/kind-up.sh first."
  exit 1
fi

kubectl config use-context "kind-$CLUSTER_NAME"

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

echo "==> Deploying backend apps..."
kubectl apply -k "$K8S_APPS"

DEPLOY_ORDER=(
  auth-service
  clinic-service
  patient-service
  followup-service
  notification-service
  api-gateway
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
echo "Test api-gateway:"
echo "  kubectl port-forward -n careflow svc/api-gateway 8080:8080"
echo "  curl http://localhost:8080/actuator/health"
