# CareFlow - arranque local (Windows)
# Uso (desde la raiz del repo): .\scripts\start-local.ps1

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

function Start-BackendService {
    param(
        [string]$Name,
        [string]$Dir,
        [int]$Port
    )
    $workDir = Join-Path $Root $Dir
    if (-not (Test-Path $workDir)) {
        Write-Warning "No encontrado: $Dir"
        return
    }
    Write-Host "==> Iniciando $Name :$Port..." -ForegroundColor Green
    $cmd = "Set-Location '$workDir'; Write-Host '$Name :$Port' -ForegroundColor Cyan; mvn spring-boot:run"
    Start-Process powershell -ArgumentList @("-NoExit", "-Command", $cmd) -WindowStyle Normal
    Start-Sleep -Seconds 2
}

function Wait-ForPort {
    param(
        [int]$Port,
        [string]$Name,
        [int]$TimeoutSeconds = 240
    )
    Write-Host "    Esperando $Name (puerto $Port)..." -ForegroundColor DarkGray
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $open = $false
        try {
            $client = New-Object System.Net.Sockets.TcpClient
            $client.Connect("127.0.0.1", $Port)
            $client.Close()
            $open = $true
        } catch {
            $open = $false
        }
        if ($open) {
            Write-Host "    $Name listo (:$Port)" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 3
    }
    Write-Warning "$Name no respondio en ${TimeoutSeconds}s. Revisa su ventana de PowerShell."
    return $false
}

Load-EnvFile (Join-Path $Root ".env")

Write-Host '==> CareFlow - arranque local' -ForegroundColor Cyan

Write-Host '==> Docker (PostgreSQL + RabbitMQ)...' -ForegroundColor Yellow
Push-Location (Join-Path $Root "infra\docker")
docker compose up -d
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "docker compose failed" }
Pop-Location

Start-Sleep -Seconds 5

# Microservicios primero; api-gateway al final (proxifica al resto).
$microservices = @(
    @{ Name = "auth-service";         Dir = "backend\auth-service";         Port = 8081 },
    @{ Name = "clinic-service";       Dir = "backend\clinic-service";       Port = 8083 },
    @{ Name = "patient-service";      Dir = "backend\patient-service";      Port = 8082 },
    @{ Name = "followup-service";     Dir = "backend\followup-service";     Port = 8084 },
    @{ Name = "notification-service"; Dir = "backend\notification-service"; Port = 8085 }
)

Write-Host '==> Iniciando microservicios (Maven puede tardar 1-2 min cada uno)...' -ForegroundColor Yellow
foreach ($svc in $microservices) {
    Start-BackendService -Name $svc.Name -Dir $svc.Dir -Port $svc.Port
}

Write-Host '==> Esperando que los microservicios escuchen en sus puertos...' -ForegroundColor Yellow
foreach ($svc in $microservices) {
    Wait-ForPort -Port $svc.Port -Name $svc.Name | Out-Null
}

Start-BackendService -Name "api-gateway" -Dir "backend\api-gateway" -Port 8080
Wait-ForPort -Port 8080 -Name "api-gateway" | Out-Null

Write-Host '==> Iniciando frontend :3000...' -ForegroundColor Green
$frontendCmd = "Set-Location '$Root\frontend'; npm run dev"
Start-Process powershell -ArgumentList @("-NoExit", "-Command", $frontendCmd) -WindowStyle Normal

Write-Host ""
Write-Host 'Listo. Abre http://localhost:3000' -ForegroundColor Cyan
Write-Host 'RabbitMQ UI: http://localhost:15672 (guest/guest)' -ForegroundColor DarkGray
Write-Host 'Cierra las ventanas de PowerShell para detener servicios Java/frontend.' -ForegroundColor DarkGray
Write-Host 'Infra Docker: .\scripts\stop-local.ps1' -ForegroundColor DarkGray
