$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$examples = Join-Path $root "examples"
$runnable = Join-Path $examples "runnable"

Write-Host "Explanation snippets:"
Get-ChildItem $examples -Recurse -File |
  Where-Object { $_.FullName -notlike "$runnable*" -and $_.Name -ne "README.md" } |
  ForEach-Object { $_.FullName.Substring($examples.Length + 1).Replace("\", "/") } |
  Sort-Object

Write-Host ""
Write-Host "Runnable Maven examples:"
Get-ChildItem $runnable -Directory |
  ForEach-Object { $_.Name } |
  Sort-Object

