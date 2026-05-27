#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $# -ne 1 ]]; then
  echo "Usage: scripts/show-example.sh <relative-example-path>"
  echo
  echo "Example:"
  echo "  scripts/show-example.sh 02-reliability/heartbeat_long_activity.java"
  exit 2
fi

FILE="$ROOT_DIR/examples/$1"

if [[ ! -f "$FILE" ]]; then
  echo "Example not found: examples/$1" >&2
  echo "Run scripts/list-examples.sh to see available examples." >&2
  exit 1
fi

case "$FILE" in
  *.java) lexer="java" ;;
  *.py) lexer="python" ;;
  *.sh) lexer="bash" ;;
  *.yaml|*.yml) lexer="yaml" ;;
  *.json) lexer="json" ;;
  *.md) lexer="markdown" ;;
  *) lexer="" ;;
esac

if command -v bat >/dev/null 2>&1; then
  bat --style=plain --language="$lexer" "$FILE"
else
  sed -n '1,240p' "$FILE"
fi

