# Day 6 - AWS and Containers

## Objectives

- Map Glue, Lambda, S3, Step Functions, EventBridge, and CloudWatch patterns to
  Temporal.
- Explain when to keep Glue Spark and when to replace orchestration glue.
- Show S3 URI references instead of payload-heavy Workflow state.
- Demonstrate containerized Worker deployment patterns.
- Explain KEDA scaling for Task Queue backlog.

## Lecture Flow

1. AWS orchestration problem and distributed checkpointing.
2. AWS primitive mapping.
3. Glue job as Activity with heartbeat.
4. Replacing S3 checkpoints with Workflow state plus S3 references.
5. Step Functions before and Temporal after.
6. Dockerfile for Java Worker.
7. Kubernetes Deployment and KEDA ScaledObject.

## Commands

Show AWS mapping:

```bash
scripts/show-example.sh 07-aws-containers/aws_mapping.md
```

Show Glue Activity:

```bash
scripts/show-example.sh 07-aws-containers/glue_activity.java
```

Show S3 reference payload pattern:

```bash
scripts/show-example.sh 07-aws-containers/s3_reference_payload.java
```

Show Step Functions JSON:

```bash
scripts/show-example.sh 07-aws-containers/step_functions_before.json
```

Show Temporal replacement:

```bash
scripts/show-example.sh 07-aws-containers/step_functions_after_temporal.java
```

Show Dockerfile:

```bash
scripts/show-example.sh 07-aws-containers/Dockerfile
```

Show Kubernetes Deployment:

```bash
scripts/show-example.sh 07-aws-containers/worker_deployment.yaml
```

Show KEDA ScaledObject:

```bash
scripts/show-example.sh 07-aws-containers/keda_scaledobject.yaml
```

Compile the AWS containers runnable project:

```bash
scripts/run-example.sh aws
```

## Windows PowerShell

```powershell
scripts/show-example.ps1 07-aws-containers/aws_mapping.md
scripts/show-example.ps1 07-aws-containers/glue_activity.java
scripts/show-example.ps1 07-aws-containers/worker_deployment.yaml
scripts/run-example.ps1 aws
```

## Instructor Notes

- Temporal replaces orchestration state, not all compute.
- Keep Glue Spark for large distributed transforms; use Temporal to supervise it.
- Keep Workflow history small. Store large files in S3 and pass references.
- Workers do not need inbound traffic for Temporal polling; health endpoints are
  for Kubernetes probes.

