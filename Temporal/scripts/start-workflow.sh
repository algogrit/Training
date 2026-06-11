#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/start-workflow.sh <task-queue> <suffix> [workflow-type] [input]

Starts a Workflow via the Temporal CLI. Useful for generating Task Queue
backlog when demoing autoscaling (Day 6 KEDA lab).

Arguments:
  task-queue      Task Queue the Workflow should be routed to
  suffix          Disambiguator appended to the Workflow ID
  workflow-type   Workflow type name (default: ImportWorkflow)
  input           JSON/quoted-string argument (default: synthetic S3 URI)

Examples:
  scripts/start-workflow.sh transform 1
  scripts/start-workflow.sh transform 42 ImportWorkflow "s3://imports-incoming/test.csv"

Loop to generate backlog (Day 6 KEDA lab):
  for i in $(seq 1 200); do scripts/start-workflow.sh transform "$i"; done
EOF
}

if [[ $# -lt 2 ]]; then
  usage
  exit 2
fi

TASK_QUEUE="$1"
SUFFIX="$2"
WF_TYPE="${3:-ImportWorkflow}"
INPUT="${4:-s3://imports-incoming/synthetic-${SUFFIX}.csv}"

if ! command -v temporal >/dev/null 2>&1; then
  echo "Temporal CLI is required. See Setup.md or 'brew install temporal'." >&2
  exit 1
fi

# bash 3.2 compatible lowercase via tr.
TYPE_LOWER=$(printf '%s' "$WF_TYPE" | tr '[:upper:]' '[:lower:]')
WORKFLOW_ID="${TYPE_LOWER}-${SUFFIX}"

temporal workflow start \
  --task-queue "$TASK_QUEUE" \
  --type "$WF_TYPE" \
  --workflow-id "$WORKFLOW_ID" \
  --input "\"$INPUT\""
