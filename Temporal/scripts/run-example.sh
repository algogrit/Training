#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'EOF'
Usage: scripts/run-example.sh <example>

Examples:
  scripts/run-example.sh hello
  scripts/run-example.sh async
  scripts/run-example.sh approval
  scripts/run-example.sh schedules
  scripts/run-example.sh testing
  scripts/run-example.sh saga

Use scripts/list-examples.sh to see all examples.
EOF
}

if [[ $# -ne 1 ]]; then
  usage
  exit 2
fi

EXAMPLE="$1"
DIR=""
MODE="exec"
MAIN_CLASS=""
NEEDS_TEMPORAL="yes"

case "$EXAMPLE" in
  hello|01|01-hello-temporal)
    DIR="examples/runnable/01-hello-temporal"
    ;;
  async|parallel|02|02-async-parallel-activities)
    DIR="examples/runnable/02-async-parallel-activities"
    ;;
  approval|signals|updates|03|03-signals-queries-updates)
    DIR="examples/runnable/03-signals-queries-updates"
    ;;
  schedules|04|04-schedules)
    DIR="examples/runnable/04-schedules"
    MODE="compile"
    MAIN_CLASS="training.temporal.schedules.CreateSchedule"
    ;;
  kafka|05|05-kafka-bridge)
    DIR="examples/runnable/05-kafka-bridge"
    MODE="compile"
    NEEDS_TEMPORAL="no"
    ;;
  testing|test|06|06-testing)
    DIR="examples/runnable/06-testing"
    MODE="test"
    NEEDS_TEMPORAL="no"
    ;;
  saga|07|07-saga)
    DIR="examples/runnable/07-saga"
    MODE="compile"
    NEEDS_TEMPORAL="no"
    ;;
  aws|containers|transform|08|08-aws-containers)
    DIR="examples/runnable/08-aws-containers"
    MODE="exec"
    MAIN_CLASS="training.temporal.aws.WorkerMain"
    ;;
  *)
    echo "Unknown runnable example: $EXAMPLE" >&2
    usage >&2
    exit 2
    ;;
esac

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required. See Setup.md." >&2
  exit 1
fi

if [[ "$NEEDS_TEMPORAL" == "yes" ]]; then
  if ! nc -z 127.0.0.1 7233 >/dev/null 2>&1; then
    echo "Temporal is not reachable at 127.0.0.1:7233." >&2
    echo "Start it in another terminal with: scripts/start-temporal.sh" >&2
    exit 1
  fi
fi

cd "$ROOT_DIR/$DIR"

case "$MODE" in
  exec)
    if [[ -n "$MAIN_CLASS" ]]; then
      mvn -q compile exec:java -Dexec.mainClass="$MAIN_CLASS"
    else
      mvn -q compile exec:java
    fi
    ;;
  test)
    mvn -q test
    ;;
  compile)
    if [[ -n "$MAIN_CLASS" ]]; then
      mvn -q compile exec:java -Dexec.mainClass="$MAIN_CLASS"
    else
      mvn -q -DskipTests compile
    fi
    ;;
esac
