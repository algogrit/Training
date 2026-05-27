#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

missing=0

require() {
  local command_name="$1"
  local install_hint="$2"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "missing: $command_name"
    echo "  $install_hint"
    missing=1
  else
    echo "ok: $command_name -> $(command -v "$command_name")"
  fi
}

require java "Install JDK 17 or newer."
require mvn "Install Maven 3.9 or newer."
if command -v temporal >/dev/null 2>&1; then
  echo "ok: temporal -> $(command -v temporal)"
elif command -v docker >/dev/null 2>&1; then
  echo "ok: docker -> $(command -v docker)"
  echo "  Temporal CLI is not installed; scripts/start-temporal.sh will use Docker."
else
  echo "missing: temporal or docker"
  echo "  Install Temporal CLI or Docker. See Setup.md."
  missing=1
fi

echo
echo "Repository: $ROOT_DIR"

if [[ "$missing" -ne 0 ]]; then
  exit 1
fi

echo
echo "All required local commands are available."
