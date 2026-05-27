$ErrorActionPreference = "Stop"

$hostName = if ($env:TEMPORAL_HOST) { $env:TEMPORAL_HOST } else { "127.0.0.1" }
$port = if ($env:TEMPORAL_PORT) { $env:TEMPORAL_PORT } else { "7233" }
$uiPort = if ($env:TEMPORAL_UI_PORT) { $env:TEMPORAL_UI_PORT } else { "8233" }

if ($null -eq (Get-Command temporal -ErrorAction SilentlyContinue)) {
  Write-Error @"
Temporal CLI was not found.

Install it first, then rerun this script:

  winget install Temporal.TemporalCLI

If winget does not have the package on your machine, use the official installer instructions:
  https://temporal.io/setup/install-temporal-cli
"@
  exit 1
}

Write-Host "Starting Temporal dev server on $hostName`:$port with Web UI on http://$hostName`:$uiPort"
temporal server start-dev --ip $hostName --port $port --ui-port $uiPort

