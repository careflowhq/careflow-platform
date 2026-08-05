# CareFlow - load auth-service image and deploy to Kind (Sprint 3.2)
# Usage (from repo root): .\scripts\kind-deploy-auth.ps1
# Prerequisites: .\scripts\kind-up.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ClusterName = "careflow-local"
$Image = "careflow/auth-service:0.0.1-SNAPSHOT"
$K8sApp = Join-Path $Root "infra\kubernetes\apps\auth-service"
$K8sSecrets = Join-Path $Root "infra\kubernetes\apps\secrets.yaml"

function Resolve-KindExecutable {
    if ($env:KIND_BIN -and (Test-Path $env:KIND_BIN)) {
        return $env:KIND_BIN
    }
    $cmd = Get-Command kind -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $fallback = "C:\ABEL\PROYECTOS\careflowhq\desarrollo\kubernetes\kind.exe"
    if (Test-Path $fallback) { return $fallback }
    throw "kind not found. Add kind to PATH or set KIND_BIN."
}

$KindExe = Resolve-KindExecutable

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$clusters = & $KindExe get clusters 2>&1 | ForEach-Object { "$_" }
$ErrorActionPreference = $prev
if ($clusters -notcontains $ClusterName) {
    throw "Cluster $ClusterName not found. Run scripts/kind-up.ps1 first."
}

kubectl config use-context "kind-$ClusterName" | Out-Null

Write-Host "==> Checking Docker image $Image..." -ForegroundColor Cyan
docker image inspect $Image 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "==> Image not found locally - building..." -ForegroundColor Yellow
    Push-Location (Join-Path $Root "infra\docker")
    docker compose -f docker-compose.build.yml build auth-service
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "docker build failed" }
    Pop-Location
}

Write-Host "==> Loading image into Kind cluster..." -ForegroundColor Green
& $KindExe load docker-image $Image --name $ClusterName
if ($LASTEXITCODE -ne 0) { throw "kind load docker-image failed" }

Write-Host "==> Deploying auth-service..." -ForegroundColor Green
kubectl apply -f $K8sSecrets
if ($LASTEXITCODE -ne 0) { throw "kubectl apply secrets failed" }
kubectl apply -k $K8sApp
if ($LASTEXITCODE -ne 0) { throw "kubectl apply failed" }

Write-Host "==> Waiting for auth-service (Ready)..." -ForegroundColor DarkGray
kubectl wait -n careflow --for=condition=ready pod `
    -l app.kubernetes.io/name=auth-service `
    --timeout=240s
if ($LASTEXITCODE -ne 0) { throw "auth-service did not become ready in time" }

Write-Host ""
Write-Host "Done. auth-service is running in namespace careflow." -ForegroundColor Green
kubectl get pods -n careflow -l app.kubernetes.io/name=auth-service
Write-Host ""
Write-Host "Test health (port-forward):" -ForegroundColor DarkGray
Write-Host "  kubectl port-forward -n careflow svc/auth-service 8081:8081"
Write-Host "  curl http://localhost:8081/actuator/health"
