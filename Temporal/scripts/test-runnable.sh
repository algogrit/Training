#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required. See Setup.md." >&2
  exit 1
fi

status=0
for pom in "$ROOT_DIR"/examples/runnable/*/pom.xml; do
  example_dir="$(dirname "$pom")"
  example_name="$(basename "$example_dir")"
  echo "==> $example_name"
  if [[ "$example_name" == "06-testing" ]]; then
    (cd "$example_dir" && mvn -q test) || status=1
  else
    (cd "$example_dir" && mvn -q -DskipTests compile) || status=1
  fi
done

exit "$status"

