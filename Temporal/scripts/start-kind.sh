#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${KIND_CLUSTER_NAME:-temporal-training}"
WORKER_IMAGE="${WORKER_IMAGE:-temporal-transform-worker:dev}"

usage() {
  cat <<EOF
Usage: scripts/start-kind.sh [up|down|load|status]

  up      Create the kind cluster (if absent) and install KEDA (default)
  down    Delete the kind cluster
  load    Build the Worker image and load it into the kind cluster
  status  Show cluster + KEDA + scaledobject status

Environment:
  KIND_CLUSTER_NAME   Cluster name (default: temporal-training)
  WORKER_IMAGE        Worker image tag (default: temporal-transform-worker:dev)
EOF
}

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing: $1" >&2
    echo "  $2" >&2
    exit 1
  fi
}

ACTION="${1:-up}"

case "$ACTION" in
  up)
    require kind   "brew install kind"
    require kubectl "brew install kubectl"
    require helm   "brew install helm"

    if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
      echo "kind cluster '${CLUSTER_NAME}' already exists. Reusing."
    else
      echo "Creating kind cluster '${CLUSTER_NAME}'..."
      kind create cluster --name "${CLUSTER_NAME}"
    fi

    kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
    kubectl cluster-info

    if ! helm repo list 2>/dev/null | awk 'NR>1 {print $1}' | grep -qx "kedacore"; then
      helm repo add kedacore https://kedacore.github.io/charts
    fi
    helm repo update >/dev/null

    if ! kubectl get namespace keda >/dev/null 2>&1; then
      kubectl create namespace keda
    fi

    if helm list -n keda --short 2>/dev/null | grep -qx "keda"; then
      echo "KEDA already installed; upgrading."
      helm upgrade keda kedacore/keda --namespace keda
    else
      helm install keda kedacore/keda --namespace keda --wait
    fi

    cat <<EOF

kind cluster '${CLUSTER_NAME}' is up and KEDA is installed.

Next steps:
  scripts/start-kind.sh load                      # build + load Worker image
  kubectl apply -f examples/runnable/08-aws-containers/k8s-worker-deployment.yaml
  kubectl apply -f examples/runnable/08-aws-containers/keda-scaledobject.yaml
EOF
    ;;

  load)
    require docker "Install Docker Desktop or Docker Engine"
    require kind   "brew install kind"

    ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    BUILD_DIR="$ROOT_DIR/examples/runnable/08-aws-containers"

    if [[ ! -f "$BUILD_DIR/Dockerfile" ]]; then
      echo "Dockerfile not found at $BUILD_DIR/Dockerfile" >&2
      exit 1
    fi

    echo "Building image ${WORKER_IMAGE} from ${BUILD_DIR}..."
    docker build -t "${WORKER_IMAGE}" "$BUILD_DIR"

    echo "Loading image into kind cluster '${CLUSTER_NAME}'..."
    kind load docker-image "${WORKER_IMAGE}" --name "${CLUSTER_NAME}"

    echo "Image ${WORKER_IMAGE} is available in the cluster."
    ;;

  down)
    require kind "brew install kind"
    if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
      kind delete cluster --name "${CLUSTER_NAME}"
    else
      echo "kind cluster '${CLUSTER_NAME}' does not exist."
    fi
    ;;

  status)
    require kubectl "brew install kubectl"
    echo "== Cluster =="
    kubectl cluster-info --context "kind-${CLUSTER_NAME}" 2>/dev/null || echo "(cluster not running)"
    echo
    echo "== KEDA =="
    kubectl get pods -n keda 2>/dev/null || echo "(keda not installed)"
    echo
    echo "== ScaledObjects =="
    kubectl get scaledobjects -A 2>/dev/null || echo "(no scaledobjects)"
    ;;

  -h|--help|help)
    usage
    ;;

  *)
    echo "Unknown action: $ACTION" >&2
    usage >&2
    exit 2
    ;;
esac
