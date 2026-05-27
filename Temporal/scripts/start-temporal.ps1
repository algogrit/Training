$ErrorActionPreference = "Stop"

$hostName = if ($env:TEMPORAL_HOST) { $env:TEMPORAL_HOST } else { "127.0.0.1" }
$port = if ($env:TEMPORAL_PORT) { $env:TEMPORAL_PORT } else { "7233" }
$uiPort = if ($env:TEMPORAL_UI_PORT) { $env:TEMPORAL_UI_PORT } else { "8233" }

if ($null -eq (Get-Command temporal -ErrorAction SilentlyContinue)) {
  if ($null -ne (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Temporal CLI was not found; starting Temporal dev server with Docker."
    Write-Host "Web UI: http://127.0.0.1:$uiPort"
    docker run --rm `
      -p "$port`:7233" `
      -p "$uiPort`:8233" `
      temporalio/temporal:latest `
      server start-dev --ip 0.0.0.0
    exit $LASTEXITCODE
  }

  Write-Error @"
Temporal CLI and Docker were not found.

Install Temporal CLI, then rerun this script:

  winget install --id Temporal.TemporalCLI --exact

Or install Docker Desktop and rerun this script.
If winget does not have the package on your machine, use the official installer instructions:
  https://temporal.io/setup/install-temporal-cli
"@
  exit 1
}

Write-Host "Starting Temporal dev server on $hostName`:$port with Web UI on http://$hostName`:$uiPort"
temporal server start-dev --ip $hostName --port $port --ui-port $uiPort
