$ErrorActionPreference = "Stop"

function Show-Usage {
  Write-Host @"
Usage: scripts/run-example.ps1 <example>

Examples:
  scripts/run-example.ps1 hello
  scripts/run-example.ps1 async
  scripts/run-example.ps1 approval
  scripts/run-example.ps1 schedules
  scripts/run-example.ps1 testing
  scripts/run-example.ps1 saga
"@
}

if ($args.Count -ne 1) {
  Show-Usage
  exit 2
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$example = $args[0]
$dir = $null
$mode = "exec"
$needsTemporal = $true
$mainClass = $null

switch ($example) {
  { $_ -in @("hello", "01", "01-hello-temporal") } {
    $dir = "examples/runnable/01-hello-temporal"
    break
  }
  { $_ -in @("async", "parallel", "02", "02-async-parallel-activities") } {
    $dir = "examples/runnable/02-async-parallel-activities"
    break
  }
  { $_ -in @("approval", "signals", "updates", "03", "03-signals-queries-updates") } {
    $dir = "examples/runnable/03-signals-queries-updates"
    break
  }
  { $_ -in @("schedules", "04", "04-schedules") } {
    $dir = "examples/runnable/04-schedules"
    $mode = "compile"
    $mainClass = "training.temporal.schedules.CreateSchedule"
    break
  }
  { $_ -in @("kafka", "05", "05-kafka-bridge") } {
    $dir = "examples/runnable/05-kafka-bridge"
    $mode = "compile"
    $needsTemporal = $false
    break
  }
  { $_ -in @("testing", "test", "06", "06-testing") } {
    $dir = "examples/runnable/06-testing"
    $mode = "test"
    $needsTemporal = $false
    break
  }
  { $_ -in @("saga", "07", "07-saga") } {
    $dir = "examples/runnable/07-saga"
    $mode = "compile"
    $needsTemporal = $false
    break
  }
  { $_ -in @("aws", "containers", "08", "08-aws-containers") } {
    $dir = "examples/runnable/08-aws-containers"
    $mode = "compile"
    $needsTemporal = $false
    break
  }
  default {
    Write-Error "Unknown runnable example: $example"
    Show-Usage
    exit 2
  }
}

if ($null -eq (Get-Command mvn -ErrorAction SilentlyContinue)) {
  Write-Error "Maven is required. See Setup.md."
  exit 1
}

if ($needsTemporal) {
  $connection = Test-NetConnection -ComputerName 127.0.0.1 -Port 7233 -WarningAction SilentlyContinue
  if (!$connection.TcpTestSucceeded) {
    Write-Error "Temporal is not reachable at 127.0.0.1:7233. Start it with: scripts/start-temporal.ps1"
    exit 1
  }
}

Push-Location (Join-Path $root $dir)
try {
  if ($mode -eq "exec") {
    mvn -q compile exec:java
  } elseif ($mode -eq "test") {
    mvn -q test
  } elseif ($mainClass) {
    mvn -q compile exec:java "-Dexec.mainClass=$mainClass"
  } else {
    mvn -q -DskipTests compile
  }
} finally {
  Pop-Location
}
