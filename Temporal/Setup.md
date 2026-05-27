# Setup

This repository contains two kinds of examples:

- `examples/<topic>`: short snippets for explanation during training.
- `examples/runnable/<name>`: Maven projects that can be compiled or run locally.

## Prerequisites

- JDK 17 or newer
- Maven 3.9 or newer
- Temporal CLI or Docker for examples that start real Workflows

## Install Prerequisites

### macOS

With Homebrew and Temporal CLI:

```bash
brew install openjdk maven temporal
```

If your shell does not find `java` after installing OpenJDK, follow Homebrew's
post-install instructions for adding the JDK to your `PATH`.

### Linux

Install a JDK and Maven with your distribution package manager, then install
either Temporal CLI or Docker.

Ubuntu/Debian example:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk maven
```

Fedora example:

```bash
sudo dnf install -y java-17-openjdk-devel maven
```

Temporal CLI:

```text
https://temporal.io/setup/install-temporal-cli
```

If you prefer Docker, install Docker Engine or Docker Desktop for Linux and use
`scripts/start-temporal.sh`.

### Windows

Use PowerShell or Windows Terminal.

With winget and Temporal CLI:

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install --id Temporal.TemporalCLI --exact
```

If `Temporal.TemporalCLI` is not available through winget on your machine, use
the official Temporal setup page:

```text
https://temporal.io/setup/install-temporal-cli
```

Windows users can also install Docker Desktop instead of Temporal CLI; the
`scripts/start-temporal.ps1` script will use Docker when `temporal.exe` is not
available. Run either the PowerShell scripts in `scripts/*.ps1`, or the Bash
scripts from WSL/Git Bash.

## Check Setup

Check your local environment:

```bash
scripts/check-local.sh
```

PowerShell:

```powershell
scripts/check-local.ps1
```

## Start Temporal Locally

Run this in a dedicated terminal. The script uses Temporal CLI when available
and falls back to Docker when available.

```bash
scripts/start-temporal.sh
```

PowerShell:

```powershell
scripts/start-temporal.ps1
```

The Temporal service listens on `127.0.0.1:7233`.
The Web UI opens at:

```text
http://127.0.0.1:8233
```

## List Examples

```bash
scripts/list-examples.sh
```

PowerShell:

```powershell
scripts/list-examples.ps1
```

Show an explanation snippet:

```bash
scripts/show-example.sh 02-reliability/heartbeat_long_activity.java
```

PowerShell:

```powershell
scripts/show-example.ps1 02-reliability/heartbeat_long_activity.java
```

## Run Runnable Examples

With Temporal running:

```bash
scripts/run-example.sh hello
scripts/run-example.sh async
scripts/run-example.sh approval
scripts/run-example.sh schedules
```

PowerShell:

```powershell
scripts/run-example.ps1 hello
scripts/run-example.ps1 async
scripts/run-example.ps1 approval
scripts/run-example.ps1 schedules
```

The in-process test example does not need a Temporal server:

```bash
scripts/run-example.sh testing
```

PowerShell:

```powershell
scripts/run-example.ps1 testing
```

Compile all runnable Maven projects:

```bash
scripts/test-runnable.sh
```

PowerShell:

```powershell
scripts/test-runnable.ps1
```

## Notes

- Kafka and AWS examples are structured to compile locally, but external systems
  are intentionally represented as focused teaching code unless the lab asks for
  Kafka, LocalStack, or Kubernetes.
- The top-level topic examples are snippets, not standalone Java programs. Use
  `scripts/show-example.sh` for those files.
- If `scripts/run-example.sh` reports that Temporal is not reachable, start it
  with `scripts/start-temporal.sh` in another terminal. On Windows, use the
  matching `.ps1` scripts.

## Reference

- Temporal CLI setup: https://temporal.io/setup/install-temporal-cli
