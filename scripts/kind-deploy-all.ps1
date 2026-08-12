# CareFlow - deploy full stack to Kind: backend + frontend + ingress (Sprint 3.4)
# Usage (from repo root): .\scripts\kind-deploy-all.ps1
# Prerequisites: .\scripts\kind-up.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ClusterName = "careflow-local"
$Version = "0.0.1-SNAPSHOT"
$K8sApps = Join-Path $Root "infra\kubernetes\apps"
$InstallIngress = Join-Path $PSScriptRoot "kind-install-ingress.ps1"

$Services = @(
    @{ Name = "auth-service"; Image = "careflow/auth-service:$Version" },
    @{ Name = "clinic-service"; Image = "careflow/clinic-service:$Version" },
    @{ Name = "patient-service"; Image = "careflow/patient-service:$Version" },
    @{ Name = "followup-service"; Image = "careflow/followup-service:$Version" },
    @{ Name = "notification-service"; Image = "careflow/notification-service:$Version" },
    @{ Name = "api-gateway"; Image = "careflow/api-gateway:$Version" },
    @{ Name = "frontend"; Image = "careflow/frontend:$Version" }
)

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

& $InstallIngress
if ($LASTEXITCODE -ne 0) { throw "ingress-nginx install failed" }

$Missing = @()
foreach ($svc in $Services) {
    docker image inspect $svc.Image 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        $Missing += $svc.Name
    }
}

if ($Missing.Count -gt 0) {
    Write-Host "==> Building missing images: $($Missing -join ', ')" -ForegroundColor Yellow
    Push-Location (Join-Path $Root "infra\docker")
    foreach ($name in $Missing) {
        docker compose -f docker-compose.build.yml build $name
        if ($LASTEXITCODE -ne 0) { Pop-Location; throw "docker build failed for $name" }
    }
    Pop-Location
}

Write-Host "==> Loading images into Kind cluster..." -ForegroundColor Green
foreach ($svc in $Services) {
    Write-Host "    $($svc.Image)" -ForegroundColor DarkGray
    & $KindExe load docker-image $svc.Image --name $ClusterName
    if ($LASTEXITCODE -ne 0) { throw "kind load docker-image failed for $($svc.Image)" }
}

Write-Host "==> Deploying apps + ingress rules (kustomize)..." -ForegroundColor Green
kubectl apply -k $K8sApps
if ($LASTEXITCODE -ne 0) { throw "kubectl apply failed" }

$DeployOrder = @(
    "auth-service",
    "clinic-service",
    "patient-service",
    "followup-service",
    "notification-service",
    "api-gateway",
    "frontend"
)

foreach ($name in $DeployOrder) {
    Write-Host "==> Waiting for $name (Ready)..." -ForegroundColor DarkGray
    kubectl wait -n careflow --for=condition=ready pod `
        -l "app.kubernetes.io/name=$name" `
        --timeout=360s
    if ($LASTEXITCODE -ne 0) { throw "$name did not become ready in time" }
}

Write-Host ""
Write-Host "Done. Full stack running in namespace careflow." -ForegroundColor Green
kubectl get pods -n careflow -l app.kubernetes.io/part-of=careflow-platform
Write-Host ""
kubectl get ingress -n careflow
Write-Host ""
Write-Host "Open in browser:" -ForegroundColor DarkGray
Write-Host "  http://localhost:8088"
Write-Host ""
Write-Host "API health via Ingress:" -ForegroundColor DarkGray
Write-Host "  curl.exe http://localhost:8088/api/auth/login"
