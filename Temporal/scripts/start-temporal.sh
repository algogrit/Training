#!/usr/bin/env bash
set -euo pipefail

HOST="${TEMPORAL_HOST:-127.0.0.1}"
PORT="${TEMPORAL_PORT:-7233}"
UI_PORT="${TEMPORAL_UI_PORT:-8233}"

if command -v temporal >/dev/null 2>&1; then
  echo "Starting Temporal dev server on $HOST:$PORT with Web UI on http://$HOST:$UI_PORT"
  exec temporal server start-dev --ip "$HOST" --port "$PORT" --ui-port "$UI_PORT"
fi

if command -v docker >/dev/null 2>&1; then
  echo "Temporal CLI was not found; starting Temporal dev server with Docker."
  echo "Web UI: http://127.0.0.1:$UI_PORT"
  exec docker run --rm \
    -p "$PORT:7233" \
    -p "$UI_PORT:8233" \
    temporalio/temporal:latest \
    server start-dev --ip 0.0.0.0
fi

cat >&2 <<'EOF'
Temporal CLI and Docker were not found.

Install Temporal CLI, then rerun this script:

  brew install temporal

Or install Docker and rerun this script. See Setup.md for platform-specific
installation options.
EOF
exit 1
