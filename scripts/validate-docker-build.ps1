# CareFlow — validate Docker image builds (Sprint 1)
# Usage (from repo root): .\scripts\validate-docker-build.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ComposeDir = Join-Path $Root "infra\docker"

Set-Location $ComposeDir

Write-Host "==> Building all CareFlow application images..." -ForegroundColor Green
docker compose -f docker-compose.build.yml build

Write-Host ""
Write-Host "==> Built images:" -ForegroundColor Green
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | Select-String "^careflow/"

Write-Host ""
Write-Host "Done. All application images built successfully." -ForegroundColor Green
