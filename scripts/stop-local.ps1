# CareFlow — detener infra Docker local
# Uso: .\scripts\stop-local.ps1

$Root = Split-Path -Parent $PSScriptRoot

Write-Host "==> Deteniendo contenedores Docker..." -ForegroundColor Yellow
Push-Location (Join-Path $Root "infra\docker")
docker compose down
Pop-Location

Write-Host "Listo. Cierra manualmente las ventanas de servicios Java y frontend si siguen abiertas." -ForegroundColor Cyan
