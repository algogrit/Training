$ErrorActionPreference = "Stop"

if ($args.Count -ne 1) {
  Write-Host "Usage: scripts/show-example.ps1 <relative-example-path>"
  Write-Host ""
  Write-Host "Example:"
  Write-Host "  scripts/show-example.ps1 02-reliability/heartbeat_long_activity.java"
  exit 2
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$file = Join-Path (Join-Path $root "examples") $args[0]

if (!(Test-Path $file -PathType Leaf)) {
  Write-Error "Example not found: examples/$($args[0])"
  exit 1
}

Get-Content $file

