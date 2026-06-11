# Temporal Training

A 24-hour (6 day × 4 hour) Java-focused Temporal training for engineers
transitioning from Airflow, Kafka, and Spring Boot stacks. Includes lecture
notes, runnable Maven labs, an Agenda, and the Docker / Kubernetes infrastructure
to run every lab on a laptop.

## What's Here

| Path                | Contents                                                            |
| ------------------- | ------------------------------------------------------------------- |
| `outline/Agenda.md` | The 6-day outline. Start here.                                      |
| `outline/detailed.md` | Long-form outline with extra context.                             |
| `lecture_notes/`    | Per-day instructor notes (talking points, demos, discussion prompts). |
| `examples/`         | Teaching snippets, organized by day.                                |
| `examples/runnable/`| Maven mini-projects for each lab.                                   |
| `docker/`           | Compose stacks for Kafka, Prometheus + Grafana, LocalStack.         |
| `scripts/`          | Bash + PowerShell entry points (the recommended way to drive labs). |
| `Setup.md`          | Detailed install + per-platform setup.                              |
| `Makefile`          | `make help` for shortcut targets over the scripts.                  |

## Quick Start

### macOS

```bash
make setup-mac      # JDK, Maven, Temporal CLI via brew
make check          # verify
make temporal       # in one terminal: start the dev server
make run-hello      # in another: run the Day 1 lab
make ui             # open the Web UI
```

For the full stack tooling (Docker, kcat, kind, kubectl, helm, awslocal):

```bash
make setup-mac-full
```

### Ubuntu / Debian

```bash
make setup-ubuntu   # apt + Temporal CLI install script (uses sudo)
# add Temporal CLI to PATH if the installer printed the hint
export PATH="$HOME/.temporalio/bin:$PATH"
make check
make temporal
make run-hello
```

For the full stack tooling (Docker, kcat, kind, kubectl, helm, awslocal):

```bash
make setup-ubuntu-full
```

### Windows

Windows users have `scripts/*.ps1` siblings for the core scripts and should
follow the Windows section in [Setup.md](Setup.md). The Make targets and
docker/kind helpers are bash-only; run them from WSL.

## Day-by-Day

Each day has lecture notes and an example folder. Days 3, 4, and 6 also pull in
the corresponding Docker stack.

| Day | Topic                          | Lecture Notes                                            | Stack required                            |
| --- | ------------------------------ | -------------------------------------------------------- | ----------------------------------------- |
| 1   | Foundations                    | [Day 1](lecture_notes/Day-01-foundations.md)             | Temporal only                             |
| 2   | Reliability + interactions     | [Day 2](lecture_notes/Day-02-reliability.md)             | Temporal only                             |
| 3   | Kafka integration              | [Day 3](lecture_notes/Day-03-kafka.md)                   | `make stack-kafka`                        |
| 4   | Production engineering         | [Day 4](lecture_notes/Day-04-production.md)              | `make stack-obs`                          |
| 5   | Saga, Spring Boot, capstone    | [Day 5](lecture_notes/Day-05-saga-spring.md)             | Temporal only                             |
| 6   | AWS migration + containers     | [Day 6](lecture_notes/Day-06-aws-containers.md)          | `make stack-aws` + `make kind-up`         |

## Running a Day's Lab

The same three-terminal pattern works for every day:

```bash
# Terminal 1: Temporal server
make temporal

# Terminal 2 (only on days that need it): the day's stack
make stack-kafka      # Day 3
make stack-obs        # Day 4
make stack-aws        # Day 6 morning
make kind-up && make kind-load   # Day 6 afternoon

# Terminal 3: the lab itself
make run-hello        # Day 1
make run-async        # Day 2 AM
make run-approval     # Day 2 PM (signals/queries/updates)
make run-schedules    # Day 2 PM (Schedules)
make run-kafka        # Day 3
make run-testing      # Day 4 (in-process; no Temporal needed)
make run-saga         # Day 5
make run-aws          # Day 6
```

Tear down between days:

```bash
make stack-down       # docker stacks (kafka, obs, localstack)
make kind-down        # delete the kind cluster
```

## Common Endpoints

| Service             | URL / address                              |
| ------------------- | ------------------------------------------ |
| Temporal gRPC       | `127.0.0.1:7233`                           |
| Temporal Web UI     | <http://127.0.0.1:8233>                    |
| Temporal metrics    | <http://127.0.0.1:7234/metrics>            |
| Kafka broker        | `localhost:9092`                           |
| Prometheus          | <http://127.0.0.1:9091>                    |
| Grafana             | <http://127.0.0.1:3000> (admin / admin)    |
| LocalStack          | <http://127.0.0.1:4566>                    |

## Make Targets

```text
make help          # full list with descriptions
make check         # verify required + optional tools
make temporal      # start Temporal dev server (foreground)
make stack-kafka   # bring up Kafka (Day 3)
make stack-obs     # bring up Prometheus + Grafana (Day 4)
make stack-aws     # bring up LocalStack (Day 6 AM)
make stack-all     # bring up every docker stack
make stack-down    # tear down docker stacks (removes volumes)
make stack-status  # docker compose ps across stacks
make kind-up       # create kind cluster + install KEDA
make kind-load     # build Worker image and load into kind
make kind-status   # show cluster + KEDA + ScaledObject state
make kind-down     # delete the kind cluster
make list          # list every example
make show FILE=... # print an example file
make run-<name>    # run a lab (hello/async/approval/schedules/kafka/testing/saga/aws)
make load-transform N=200  # start N Workflows on `transform` (KEDA demo)
make test          # compile every runnable project
make build         # package every runnable project
make clean         # mvn clean every runnable project
```

## Going Further

- Examples are intentionally small. The Agenda describes the conceptual arc; the
  lecture notes turn each Agenda bullet into a teachable demo.
- Snippet files in `examples/<topic>/` often omit imports for teaching clarity.
  See `examples/README.md`.
- The Day 6 K8s lab assumes a single-node `kind` cluster; the Worker image is
  built from `examples/runnable/08-aws-containers/`.
- Replay-test capture: `temporal workflow show --workflow-id X --output json` to
  snapshot a production history for use with `WorkflowReplayer` (Day 4).
