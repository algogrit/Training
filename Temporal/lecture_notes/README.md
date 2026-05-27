# Lecture Notes

Use these notes as the instructor runbook. They pair the outline with concrete
commands and example files.

## Before Class

Verify local tooling:

```bash
scripts/check-local.sh
```

Windows PowerShell:

```powershell
scripts/check-local.ps1
```

Start Temporal in a dedicated terminal:

```bash
scripts/start-temporal.sh
```

Windows PowerShell:

```powershell
scripts/start-temporal.ps1
```

Open the Web UI:

```text
http://127.0.0.1:8233
```

List examples:

```bash
scripts/list-examples.sh
```

Verify runnable examples:

```bash
scripts/test-runnable.sh
```

## Day Guides

- [Day 1 - Foundations](Day-01-foundations.md)
- [Day 2 - Reliability](Day-02-reliability.md)
- [Day 3 - Kafka](Day-03-kafka.md)
- [Day 4 - Production](Day-04-production.md)
- [Day 5 - Saga and Spring](Day-05-saga-spring.md)
- [Day 6 - AWS and Containers](Day-06-aws-containers.md)

