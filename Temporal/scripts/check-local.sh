#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

missing=0

require() {
  local name="$1"
  local hint="$2"
  if command -v "$name" >/dev/null 2>&1; then
    printf "  ok   %-10s -> %s\n" "$name" "$(command -v "$name")"
  else
    printf "  MISS %-10s   %s\n" "$name" "$hint"
    missing=1
  fi
}

optional() {
  local name="$1"
  local used_for="$2"
  local hint="$3"
  if command -v "$name" >/dev/null 2>&1; then
    printf "  ok   %-10s -> %s\n" "$name" "$(command -v "$name")"
  else
    printf "  --   %-10s   (optional; %s)  %s\n" "$name" "$used_for" "$hint"
  fi
}

echo "== Required =="
require java "Install JDK 17 or newer. macOS: brew install openjdk@17"
require mvn  "Install Maven 3.9+. macOS: brew install maven"

# At least one of temporal CLI or docker is required for the Temporal server.
if command -v temporal >/dev/null 2>&1; then
  printf "  ok   %-10s -> %s\n" "temporal" "$(command -v temporal)"
elif command -v docker >/dev/null 2>&1; then
  printf "  ok   %-10s -> %s   (Temporal CLI not found; start-temporal.sh will use docker)\n" \
    "docker" "$(command -v docker)"
else
  printf "  MISS temporal/docker   Install Temporal CLI ('brew install temporal') or Docker.\n"
  missing=1
fi

echo
echo "== Day 3-6 stacks (docker) =="
if command -v docker >/dev/null 2>&1; then
  if docker compose version >/dev/null 2>&1; then
    printf "  ok   %-10s -> v2 plugin present\n" "compose"
  else
    printf "  MISS %-10s   Install Docker Desktop or the compose v2 plugin\n" "compose"
    missing=1
  fi
else
  printf "  --   %-10s   (optional; needed for Day 3 Kafka, Day 4 obs, Day 6 LocalStack)\n" "docker"
fi

echo
echo "== Day 3 Kafka labs =="
optional kcat "Day 3 Kafka prod/consume" "brew install kcat"

echo
echo "== Day 6 container labs =="
optional kind    "Day 6 K8s lab" "brew install kind"
optional kubectl "Day 6 K8s lab" "brew install kubectl"
optional helm    "Day 6 KEDA install" "brew install helm"
optional awslocal "Day 6 LocalStack interaction" "pipx install awscli-local"

echo
echo "Repository: $ROOT_DIR"

if [[ "$missing" -ne 0 ]]; then
  echo
  echo "One or more required tools are missing. See Setup.md."
  exit 1
fi

echo
echo "Required tools present. Optional tools above are only needed for the labs they list."
