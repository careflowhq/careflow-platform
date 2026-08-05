#!/usr/bin/env bash
# CareFlow — create Kind cluster and deploy infra (Sprint 3.1)
# Usage (from repo root): ./scripts/kind-up.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KIND_CONFIG="$ROOT/infra/kind/careflow-kind.yaml"
K8S_BASE="$ROOT/infra/kubernetes/base"
CLUSTER_NAME="careflow-local"

echo "==> CareFlow Kind — Fase 3.0 + 3.1"

if ! docker info >/dev/null 2>&1; then
  echo "!! Docker is not running. Start Docker Desktop and retry."
  exit 1
fi

if kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
  echo "==> Cluster '$CLUSTER_NAME' already exists — reusing"
else
  echo "==> Creating Kind cluster '$CLUSTER_NAME'..."
  kind create cluster --name "$CLUSTER_NAME" --config "$KIND_CONFIG"
fi

kubectl config use-context "kind-$CLUSTER_NAME"

echo "==> Deploying namespace + postgres + rabbitmq..."
kubectl apply -k "$K8S_BASE"

echo "==> Waiting for postgres..."
sleep 5
kubectl wait -n careflow --for=condition=ready pod -l app.kubernetes.io/name=postgres --timeout=180s

echo "==> Waiting for rabbitmq..."
kubectl wait -n careflow --for=condition=ready pod -l app.kubernetes.io/name=rabbitmq --timeout=180s

echo ""
echo "Done. Cluster: kind-$CLUSTER_NAME | Namespace: careflow"
kubectl get pods -n careflow
