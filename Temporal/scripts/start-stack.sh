#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_DIR="$ROOT_DIR/docker"

usage() {
  cat <<'EOF'
Usage: scripts/start-stack.sh <stack> [up|down|logs|status]

Stacks:
  kafka    Kafka KRaft single-broker on localhost:9092          (Day 3 labs)
  obs      Prometheus on :9091 + Grafana on :3000               (Day 4 lab)
  aws      LocalStack (S3, SQS, Glue, IAM, STS) on :4566        (Day 6 AM)
  all      All of the above

Actions:
  up       docker compose up -d (default)
  down     docker compose down -v
  logs     docker compose logs -f
  status   docker compose ps

Examples:
  scripts/start-stack.sh kafka              # bring up Kafka
  scripts/start-stack.sh obs up
  scripts/start-stack.sh aws status
  scripts/start-stack.sh all down
EOF
}

if [[ $# -lt 1 ]]; then
  usage
  exit 2
fi

STACK="$1"
ACTION="${2:-up}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required. See Setup.md." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose v2 plugin is required." >&2
  echo "  brew install docker            # Docker Desktop bundles compose v2" >&2
  exit 1
fi

# Use an array for compose -f flags; bash 3.2 compatible.
FILES=()
case "$STACK" in
  kafka)
    FILES=(-f "$COMPOSE_DIR/compose.kafka.yml")
    ;;
  obs|observability)
    FILES=(-f "$COMPOSE_DIR/compose.observability.yml")
    ;;
  aws|localstack)
    FILES=(-f "$COMPOSE_DIR/compose.localstack.yml")
    ;;
  all)
    FILES=(
      -f "$COMPOSE_DIR/compose.kafka.yml"
      -f "$COMPOSE_DIR/compose.observability.yml"
      -f "$COMPOSE_DIR/compose.localstack.yml"
    )
    ;;
  *)
    echo "Unknown stack: $STACK" >&2
    usage >&2
    exit 2
    ;;
esac

# Run compose from the docker/ directory so relative volume paths resolve.
cd "$COMPOSE_DIR"

case "$ACTION" in
  up)
    docker compose "${FILES[@]}" up -d
    echo
    docker compose "${FILES[@]}" ps
    ;;
  down)
    docker compose "${FILES[@]}" down -v
    ;;
  logs)
    docker compose "${FILES[@]}" logs -f
    ;;
  status|ps)
    docker compose "${FILES[@]}" ps
    ;;
  *)
    echo "Unknown action: $ACTION" >&2
    usage >&2
    exit 2
    ;;
esac
