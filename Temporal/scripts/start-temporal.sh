#!/usr/bin/env bash
set -euo pipefail

HOST="${TEMPORAL_HOST:-127.0.0.1}"
PORT="${TEMPORAL_PORT:-7233}"
UI_PORT="${TEMPORAL_UI_PORT:-8233}"
METRICS_PORT="${TEMPORAL_METRICS_PORT:-7234}"

if command -v temporal >/dev/null 2>&1; then
  echo "Starting Temporal dev server"
  echo "  gRPC:     $HOST:$PORT"
  echo "  Web UI:   http://$HOST:$UI_PORT"
  echo "  Metrics:  http://$HOST:$METRICS_PORT/metrics"
  exec temporal server start-dev \
    --ip "$HOST" \
    --port "$PORT" \
    --ui-port "$UI_PORT" \
    --metrics-port "$METRICS_PORT"
fi

if command -v docker >/dev/null 2>&1; then
  echo "Temporal CLI was not found; starting Temporal dev server with Docker."
  echo "  Web UI:   http://127.0.0.1:$UI_PORT"
  echo "  Metrics:  http://127.0.0.1:$METRICS_PORT/metrics"
  exec docker run --rm \
    -p "$PORT:7233" \
    -p "$UI_PORT:8233" \
    -p "$METRICS_PORT:7234" \
    temporalio/temporal:latest \
    server start-dev --ip 0.0.0.0 --metrics-port 7234
fi

cat >&2 <<'EOF'
Temporal CLI and Docker were not found.

Install Temporal CLI, then rerun this script:

  brew install temporal

Or install Docker and rerun this script. See Setup.md for platform-specific
installation options.
EOF
exit 1
