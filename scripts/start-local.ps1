# CareFlow — arranque local (Windows)
# Uso: .\scripts\start-local.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

function Load-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return }
    Get-Content $Path | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $parts = $_ -split '=', 2
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

Load-EnvFile (Join-Path $Root ".env")

Write-Host "==> CareFlow — arranque local" -ForegroundColor Cyan

Write-Host "==> Docker (PostgreSQL + RabbitMQ)..." -ForegroundColor Yellow
Push-Location (Join-Path $Root "infra\docker")
docker compose up -d
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "docker compose failed" }
Pop-Location

Start-Sleep -Seconds 3

$backendServices = @(
    @{ Name = "api-gateway";         Dir = "backend\api-gateway";         Port = 8080 },
    @{ Name = "auth-service";         Dir = "backend\auth-service";         Port = 8081 },
    @{ Name = "patient-service";      Dir = "backend\patient-service";      Port = 8082 },
    @{ Name = "clinic-service";       Dir = "backend\clinic-service";       Port = 8083 },
    @{ Name = "followup-service";     Dir = "backend\followup-service";     Port = 8084 },
    @{ Name = "notification-service"; Dir = "backend\notification-service"; Port = 8085 }
)

foreach ($svc in $backendServices) {
    $workDir = Join-Path $Root $svc.Dir
    if (-not (Test-Path $workDir)) {
        Write-Warning "No encontrado: $($svc.Dir)"
        continue
    }
    Write-Host "==> Iniciando $($svc.Name) :$($svc.Port)..." -ForegroundColor Green
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$workDir'; Write-Host '$($svc.Name) :$($svc.Port)' -ForegroundColor Cyan; mvn spring-boot:run"
    ) -WindowStyle Normal
    Start-Sleep -Milliseconds 800
}

Write-Host "==> Iniciando frontend :3000..." -ForegroundColor Green
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$Root\frontend'; npm run dev"
) -WindowStyle Normal

Write-Host ""
Write-Host "Listo. Abre http://localhost:3000" -ForegroundColor Cyan
Write-Host "RabbitMQ UI: http://localhost:15672 (guest/guest)" -ForegroundColor DarkGray
Write-Host "Cierra las ventanas de PowerShell para detener servicios Java/frontend." -ForegroundColor DarkGray
Write-Host "Infra Docker: .\scripts\stop-local.ps1" -ForegroundColor DarkGray
