$ErrorActionPreference = "Stop"

$missing = $false

function Test-Command {
  param(
    [string] $Name,
    [string] $Hint
  )

  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if ($null -eq $command) {
    Write-Host "missing: $Name"
    Write-Host "  $Hint"
    $script:missing = $true
  } else {
    Write-Host "ok: $Name -> $($command.Source)"
  }
}

Test-Command "java" "Install JDK 17 or newer."
Test-Command "mvn" "Install Maven 3.9 or newer."
if ($null -ne (Get-Command temporal -ErrorAction SilentlyContinue)) {
  $command = Get-Command temporal
  Write-Host "ok: temporal -> $($command.Source)"
} elseif ($null -ne (Get-Command docker -ErrorAction SilentlyContinue)) {
  $command = Get-Command docker
  Write-Host "ok: docker -> $($command.Source)"
  Write-Host "  Temporal CLI is not installed; scripts/start-temporal.ps1 will use Docker."
} else {
  Write-Host "missing: temporal or docker"
  Write-Host "  Install Temporal CLI or Docker. See Setup.md."
  $missing = $true
}

if ($missing) {
  exit 1
}

Write-Host ""
Write-Host "All required local commands are available."
