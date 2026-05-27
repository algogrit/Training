#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_ID="${1:-hello-temporal-demo}"

temporal workflow show \
  --workflow-id "$WORKFLOW_ID" \
  --output json

