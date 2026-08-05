# CareFlow — delete Kind cluster
# Usage (from repo root): .\scripts\kind-down.ps1

$ErrorActionPreference = "Stop"
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

Write-Host "==> Deleting Kind cluster '$ClusterName'..." -ForegroundColor Yellow
& $KindExe delete cluster --name $ClusterName

Write-Host "Done." -ForegroundColor Green
