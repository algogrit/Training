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
require temporal "Install with: brew install temporal"

echo
echo "Repository: $ROOT_DIR"

if [[ "$missing" -ne 0 ]]; then
  exit 1
fi

echo
echo "All required local commands are available."

