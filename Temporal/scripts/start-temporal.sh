#!/usr/bin/env bash
set -euo pipefail

HOST="${TEMPORAL_HOST:-127.0.0.1}"
PORT="${TEMPORAL_PORT:-7233}"
UI_PORT="${TEMPORAL_UI_PORT:-8233}"

if command -v temporal >/dev/null 2>&1; then
  echo "Starting Temporal dev server on $HOST:$PORT with Web UI on http://$HOST:$UI_PORT"
  exec temporal server start-dev --ip "$HOST" --port "$PORT" --ui-port "$UI_PORT"
fi

cat >&2 <<'EOF'
Temporal CLI was not found.

Install it first, then rerun this script:

  brew install temporal

Or see Setup.md for other installation options.
EOF
exit 1

