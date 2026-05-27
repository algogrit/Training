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
Test-Command "temporal" "Install Temporal CLI from https://temporal.io/setup/install-temporal-cli"

if ($missing) {
  exit 1
}

Write-Host ""
Write-Host "All required local commands are available."

