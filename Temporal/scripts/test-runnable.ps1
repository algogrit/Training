$ErrorActionPreference = "Stop"

if ($null -eq (Get-Command mvn -ErrorAction SilentlyContinue)) {
  Write-Error "Maven is required. See Setup.md."
  exit 1
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$runnable = Join-Path $root "examples/runnable"
$failed = $false

Get-ChildItem $runnable -Directory | Sort-Object Name | ForEach-Object {
  Write-Host "==> $($_.Name)"
  Push-Location $_.FullName
  try {
    if ($_.Name -eq "06-testing") {
      mvn -q test
    } else {
      mvn -q -DskipTests compile
    }
    if ($LASTEXITCODE -ne 0) {
      $failed = $true
    }
  } finally {
    Pop-Location
  }
}

if ($failed) {
  exit 1
}

