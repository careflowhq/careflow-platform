#!/usr/bin/env bash
# CareFlow — install ingress-nginx controller for Kind (Sprint 3.4)
# Usage (from repo root): ./scripts/kind-install-ingress.sh

set -euo pipefail

INGRESS_NGINX_VERSION="controller-v1.11.3"
INGRESS_MANIFEST="https://raw.githubusercontent.com/kubernetes/ingress-nginx/${INGRESS_NGINX_VERSION}/deploy/static/provider/kind/deploy.yaml"

echo "==> Installing ingress-nginx (${INGRESS_NGINX_VERSION}) for Kind..."
kubectl apply -f "$INGRESS_MANIFEST"

echo "==> Waiting for ingress-nginx controller..."
kubectl wait -n ingress-nginx --for=condition=ready pod \
  -l app.kubernetes.io/component=controller --timeout=180s

echo ""
echo "Done. Ingress available on http://localhost:8088"
kubectl get pods -n ingress-nginx
