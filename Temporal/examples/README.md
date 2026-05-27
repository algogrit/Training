# Temporal Java Examples

Small examples that line up with the training outline. Most directories are
explanation snippets: they are meant to be shown during lectures before students
turn the idea into a complete lab. Runnable mini-projects are included where the
outline calls for hands-on work.

## Running

See [Setup.md](../Setup.md) for macOS, Linux, and Windows setup.

List examples:

```bash
scripts/list-examples.sh
```

Show a teaching snippet:

```bash
scripts/show-example.sh 02-reliability/heartbeat_long_activity.java
```

Run a Maven example:

```bash
scripts/start-temporal.sh
scripts/run-example.sh hello
```

## Map to the Outline

- `01-foundations`: Airflow DAG versus Temporal Workflow, core primitives, replay-safe code.
- `02-reliability`: Async Activity calls, partial failure, cancellation, retries, heartbeats, time.
- `03-interactions`: Signals, Queries, Updates, Schedules, workflow timeouts, Child Workflows.
- `04-kafka`: Kafka bridge, Kafka Activity, producer Activity, partition fan-out, DLQ routing.
- `05-production`: Versioning, worker sizing, observability, namespaces, replay testing.
- `06-saga-spring`: Saga compensation, sync/async Saga APIs, Spring bean wiring, continue-as-new.
- `07-aws-containers`: AWS primitive mapping, Glue wrapper, S3 references, Docker, Kubernetes, KEDA.
- `runnable`: Complete mini-projects for labs and live demos.

## Format

Snippet files intentionally optimize for teaching clarity over complete
application structure. They often omit package declarations, imports, or concrete
infrastructure setup so the important Temporal concept stays visible.
