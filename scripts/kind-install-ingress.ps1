# CareFlow - install ingress-nginx controller for Kind (Sprint 3.4)
# Usage (from repo root): .\scripts\kind-install-ingress.ps1
# Idempotent: safe to re-run on an existing cluster.

$ErrorActionPreference = "Stop"

# Pinned manifest from https://github.com/kubernetes/ingress-nginx
$IngressNginxVersion = "controller-v1.11.3"
$IngressManifest = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/$IngressNginxVersion/deploy/static/provider/kind/deploy.yaml"

Write-Host "==> Installing ingress-nginx ($IngressNginxVersion) for Kind..." -ForegroundColor Cyan
kubectl apply -f $IngressManifest
if ($LASTEXITCODE -ne 0) { throw "kubectl apply ingress-nginx failed" }

Write-Host "==> Waiting for ingress-nginx controller (Ready)..." -ForegroundColor DarkGray
kubectl wait -n ingress-nginx --for=condition=ready pod `
    -l app.kubernetes.io/component=controller `
    --timeout=180s
if ($LASTEXITCODE -ne 0) { throw "ingress-nginx controller did not become ready in time" }

Write-Host ""
Write-Host "Done. Ingress available on http://localhost:8088 (Kind hostPort mapping)." -ForegroundColor Green
kubectl get pods -n ingress-nginx
