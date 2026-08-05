# CareFlow — create Kind cluster and deploy infra (Sprint 3.1)
# Usage (from repo root): .\scripts\kind-up.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$KindConfig = Join-Path $Root "infra\kind\careflow-kind.yaml"
$K8sBase = Join-Path $Root "infra\kubernetes\base"
$ClusterName = "careflow-local"

function Resolve-KindExecutable {
    if ($env:KIND_BIN -and (Test-Path $env:KIND_BIN)) {
        return $env:KIND_BIN
    }
    $cmd = Get-Command kind -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $fallback = "C:\ABEL\PROYECTOS\careflowhq\desarrollo\kubernetes\kind.exe"
    if (Test-Path $fallback) { return $fallback }
    throw "kind not found. Add kind to PATH or set KIND_BIN to the executable path."
}

$KindExe = Resolve-KindExecutable

function Test-DockerRunning {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not running. Start Docker Desktop and retry."
    }
}

function Test-ClusterExists {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $clusters = & $KindExe get clusters 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = $prev
    return ($clusters -contains $ClusterName)
}

Write-Host "==> CareFlow Kind - Fase 3.0 + 3.1" -ForegroundColor Cyan

Test-DockerRunning

if (Test-ClusterExists) {
    Write-Host "==> Cluster '$ClusterName' already exists - reusing" -ForegroundColor Yellow
} else {
    Write-Host "==> Creating Kind cluster '$ClusterName'..." -ForegroundColor Green
    & $KindExe create cluster --name $ClusterName --config $KindConfig
    if ($LASTEXITCODE -ne 0) { throw "kind create cluster failed" }
}

kubectl cluster-info --context "kind-$ClusterName" | Out-Null
kubectl config use-context "kind-$ClusterName" | Out-Null

Write-Host "==> Deploying namespace + postgres + rabbitmq..." -ForegroundColor Green
kubectl apply -k $K8sBase
if ($LASTEXITCODE -ne 0) { throw "kubectl apply failed" }

Write-Host "==> Waiting for postgres (Ready)..." -ForegroundColor DarkGray
Start-Sleep -Seconds 5
kubectl wait -n careflow --for=condition=ready pod `
    -l app.kubernetes.io/name=postgres `
    --timeout=180s
if ($LASTEXITCODE -ne 0) { throw "postgres did not become ready in time" }

Write-Host "==> Waiting for rabbitmq (Ready)..." -ForegroundColor DarkGray
kubectl wait -n careflow --for=condition=ready pod `
    -l app.kubernetes.io/name=rabbitmq `
    --timeout=180s
if ($LASTEXITCODE -ne 0) { throw "rabbitmq did not become ready in time" }

Write-Host ""
Write-Host "Done. Cluster: kind-$ClusterName | Namespace: careflow" -ForegroundColor Green
Write-Host ""
kubectl get pods -n careflow
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor DarkGray
Write-Host "  kubectl get all -n careflow"
Write-Host "  k9s -n careflow"
Write-Host "  .\scripts\kind-down.ps1   # delete cluster"
