#!/usr/bin/env bash
# CareFlow — delete Kind cluster
# Usage (from repo root): ./scripts/kind-down.sh

set -euo pipefail

CLUSTER_NAME="careflow-local"

echo "==> Deleting Kind cluster '$CLUSTER_NAME'..."
kind delete cluster --name "$CLUSTER_NAME"
echo "Done."
