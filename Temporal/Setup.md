# Setup

This repository contains two kinds of examples:

- `examples/<topic>/` — short snippets for explanation during training.
- `examples/runnable/<name>/` — Maven projects that can be compiled or run locally.

Most labs run on a single laptop. Days 3, 4, and 6 use additional Docker Compose
stacks (Kafka, Prometheus + Grafana, LocalStack) and a local Kubernetes cluster
(`kind` + KEDA).

## Prerequisites

**Required for every day:**

- JDK 17 or newer
- Maven 3.9 or newer
- Temporal CLI or Docker (the start script picks whichever is present)

**Required for specific days:**

| Day                                  | Additional tools                                     |
| ------------------------------------ | ---------------------------------------------------- |
| Day 3 (Kafka)                        | Docker Compose v2; `kcat` for poking the broker      |
| Day 4 (Observability)                | Docker Compose v2 (Prometheus + Grafana)             |
| Day 6 AM (AWS migration)             | Docker Compose v2 (LocalStack); `awslocal` CLI       |
| Day 6 PM (Containers + KEDA)         | `kind`, `kubectl`, `helm`                            |

`scripts/check-local.sh` reports which tools are present and which are missing.

## Install Prerequisites

### macOS

Required:

```bash
brew install openjdk maven temporal
```

Optional stacks (install only those you need):

```bash
brew install docker          # Docker Desktop (provides compose v2)
brew install kcat            # Day 3 Kafka labs
brew install kind            # Day 6 K8s lab
brew install kubectl         # Day 6 K8s lab
brew install helm            # Day 6 KEDA install
pipx install awscli-local    # Day 6 LocalStack (awslocal)
```

If your shell does not find `java` after installing OpenJDK, follow Homebrew's
post-install instructions for adding the JDK to your `PATH`.

### Linux (Ubuntu / Debian)

A Makefile target installs the required tools (JDK 17, Maven, Temporal CLI):

```bash
make setup-ubuntu
```

Full setup including Docker, `kcat`, `kind`, `kubectl`, `helm`, and `awscli-local`:

```bash
make setup-ubuntu-full
```

Notes:

- Both targets use `sudo`. Read the Makefile if you'd rather do it by hand.
- The Temporal CLI installs to `~/.temporalio/bin` via the official install
  script. Add that to your `PATH`:
  `export PATH="$HOME/.temporalio/bin:$PATH"`.
- `setup-ubuntu-full` adds you to the `docker` group; log out and back in for
  it to take effect.
- `kcat`'s package name changed: 22.04+ has `kcat`, older releases have
  `kafkacat`. The target tries both.

### Linux (Fedora / RHEL / other)

```bash
sudo dnf install -y java-17-openjdk-devel maven
curl -sSf https://temporal.download/cli.sh | sh
```

For Docker, follow Docker Engine's official Fedora instructions and install the
compose v2 plugin. `kind`, `kubectl`, and `helm` use the same official binary
installers as the Ubuntu target (curl into `/usr/local/bin`).

### Windows

Use PowerShell or Windows Terminal.

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install --id Temporal.TemporalCLI --exact
winget install Docker.DockerDesktop          # required for stacks
```

If `Temporal.TemporalCLI` is not available through winget, use
<https://temporal.io/setup/install-temporal-cli>.

Windows users can run either the PowerShell scripts in `scripts/*.ps1`, or the
Bash scripts from WSL/Git Bash. Note that the docker-stack and kind helpers are
currently bash-only; on Windows, run them from WSL.

## Check Setup

```bash
scripts/check-local.sh
```

PowerShell:

```powershell
scripts/check-local.ps1
```

## Start the Temporal Server

Run this in a dedicated terminal. The script uses Temporal CLI when available
and falls back to Docker when available.

```bash
scripts/start-temporal.sh
```

PowerShell:

```powershell
scripts/start-temporal.ps1
```

| Endpoint                        | URL                              |
| ------------------------------- | -------------------------------- |
| gRPC frontend                   | `127.0.0.1:7233`                 |
| Web UI                          | <http://127.0.0.1:8233>          |
| Prometheus metrics              | <http://127.0.0.1:7234/metrics>  |

## Start Optional Stacks (Days 3, 4, 6)

A single dispatcher brings up each stack via Docker Compose. The compose files
live under `docker/`.

```bash
scripts/start-stack.sh kafka       # Day 3:  Kafka on :9092
scripts/start-stack.sh obs         # Day 4:  Prometheus :9091 + Grafana :3000
scripts/start-stack.sh aws         # Day 6:  LocalStack on :4566 (S3, SQS, Glue)
scripts/start-stack.sh all         # bring up all three
scripts/start-stack.sh kafka down  # tear down (also: status, logs)
```

| Service                 | Host port | Notes                                       |
| ----------------------- | --------- | ------------------------------------------- |
| Kafka (KRaft, 1 broker) | 9092      | Auto-create topics enabled                  |
| Prometheus              | 9091      | Scrapes Temporal server + Worker (:9464)    |
| Grafana                 | 3000      | admin / admin; anonymous viewer allowed     |
| LocalStack              | 4566      | S3, SQS, Glue, IAM, STS, CloudWatch Logs    |

## Start the Local Kubernetes Cluster (Day 6 PM)

A separate helper drives `kind` and KEDA:

```bash
scripts/start-kind.sh up      # create cluster, install KEDA
scripts/start-kind.sh load    # build the Worker image and load into kind
scripts/start-kind.sh status  # show cluster + KEDA + ScaledObject state
scripts/start-kind.sh down    # delete the cluster
```

After `up` + `load`:

```bash
kubectl apply -f examples/runnable/08-aws-containers/k8s-worker-deployment.yaml
kubectl apply -f examples/runnable/08-aws-containers/keda-scaledobject.yaml
```

## List Examples

```bash
scripts/list-examples.sh
```

Show a teaching snippet:

```bash
scripts/show-example.sh 02-reliability/heartbeat_long_activity.java
```

## Run Runnable Examples

With Temporal running:

```bash
scripts/run-example.sh hello       # Day 1
scripts/run-example.sh async       # Day 2 morning
scripts/run-example.sh approval    # Day 2 afternoon
scripts/run-example.sh schedules   # Day 2 afternoon
scripts/run-example.sh kafka       # Day 3 (also needs start-stack.sh kafka)
scripts/run-example.sh saga        # Day 5
scripts/run-example.sh aws         # Day 6 Worker (also needs start-stack.sh aws)
```

The in-process test example does not need a Temporal server:

```bash
scripts/run-example.sh testing
```

Compile all runnable Maven projects:

```bash
scripts/test-runnable.sh
```

## Generate Workflow Load (Day 6 KEDA Demo)

```bash
for i in $(seq 1 200); do scripts/start-workflow.sh transform "$i"; done
```

## Makefile Targets

If you prefer `make`:

```bash
make help          # list all targets
make check         # scripts/check-local.sh
make temporal      # scripts/start-temporal.sh
make stack-kafka   # scripts/start-stack.sh kafka
make stack-obs     # scripts/start-stack.sh obs
make stack-aws     # scripts/start-stack.sh aws
make stack-down    # tear down all stacks
make kind-up       # scripts/start-kind.sh up
make kind-load     # scripts/start-kind.sh load
make kind-down     # scripts/start-kind.sh down
make run-hello     # scripts/run-example.sh hello
make test          # compile every runnable project
make clean         # mvn clean for every runnable project
```

## Notes

- Kafka and AWS snippets in `examples/<topic>/` are intentionally focused
  teaching code. Full end-to-end labs live in `examples/runnable/`.
- The top-level topic files are snippets, not standalone Java programs. Use
  `scripts/show-example.sh` for those.
- If `scripts/run-example.sh` reports that Temporal is not reachable, start it
  with `scripts/start-temporal.sh` in another terminal.

## Reference

- Temporal CLI setup: <https://temporal.io/setup/install-temporal-cli>
- Bitnami Kafka image: <https://hub.docker.com/r/bitnami/kafka>
- LocalStack: <https://docs.localstack.cloud/>
- KEDA Temporal scaler: <https://keda.sh/docs/latest/scalers/temporal/>
