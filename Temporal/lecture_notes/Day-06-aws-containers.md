# Day 6 - AWS Migration & Container Workloads

## Objectives

- Map AWS orchestration primitives (Lambda, Glue, Step Functions, EventBridge, S3) to Temporal.
- Decide when to keep AWS compute vs replace it.
- Wrap a Glue Spark job as a Temporal Activity with proper heartbeat + backoff.
- Replace S3-based checkpointing with Workflow state + S3 *references*.
- Containerize a Java Worker correctly (multi-stage, container-aware JVM, exec health probes).
- Deploy Workers to Kubernetes with rolling updates aligned to Activity timeouts.
- Autoscale Worker pools with KEDA using the **native Temporal scaler** (no Prometheus exporter required).
- Compare Temporal Cloud vs self-hosted on EKS.

## Prerequisites Check (5 min)

- LocalStack is up: `scripts/start-stack.sh aws` and `awslocal s3 ls` responds.
- `kind` cluster + KEDA are up: `scripts/start-kind.sh up` and
  `scripts/start-kind.sh status` shows the cluster green and `keda-operator`
  running.
- Worker image built and loaded into kind: `scripts/start-kind.sh load`.

If any are red, the afternoon's containerized lab won't run. Fix now.

---

## Morning - 2 hrs - Replacing AWS Glue + Lambda + S3 with Temporal

### 1. The AWS orchestration problem (20 min) `[aws]`

**Talking point.** A typical AWS-native pipeline assembles itself from independent services that each own a fragment of state:

- **EventBridge** rule fires on schedule or event.
- **Lambda** validates, transforms, sometimes orchestrates.
- **Step Functions** declares state transitions in JSON.
- **Glue** runs Spark/Python shell jobs, writes results to S3.
- **S3** is the handoff medium - checkpoints, intermediate results, "the bag of state."

The hidden cost: every handoff is a chance for state to disagree. Step Functions thinks the Glue job succeeded; S3 has no output; Lambda errored before writing the checkpoint; CloudWatch shows three different timestamps. Recovery is a runbook, not a button.

Temporal flips the model: one Workflow owns the state machine; AWS services become *compute* the Workflow calls into. The Workflow's history is the audit trail.

**Discussion prompt.** "Today, when a Step Functions execution fails at the Glue step, what do you do?" Most answers describe console diving + S3 spelunking + manual restart. Set the hook: Temporal records the Glue job's `runId`, retries by resuming the poll, and surfaces the failure as a typed Activity error.

### 2. Mapping AWS primitives to Temporal (20 min) `[aws]`

```bash
scripts/show-example.sh 07-aws-containers/aws_mapping.md
```

| Existing AWS shape                                                | Temporal shape                                              |
| ----------------------------------------------------------------- | ----------------------------------------------------------- |
| EventBridge rule invokes Lambda that starts Step Functions        | Consumer (or thin Lambda) `signalWithStart`s a Workflow     |
| Step Functions branches through JSON states                       | Workflow branches through Java code                         |
| Glue Python shell job writes checkpoints to S3                    | Activity returns result; Workflow history records whether it ran |
| Glue Spark job does heavy distributed transform                   | Activity starts the Glue job and heartbeats `runId` while polling |
| CloudWatch retry on Lambda                                        | Activity `RetryOptions` with typed `ApplicationFailure`     |
| S3 handoff file between Lambdas                                   | Activity returns S3 URI; Workflow owns sequencing           |
| DynamoDB checkpoint table for "did this step run?"                | Workflow event history (built in)                           |

**Walking through the table.** Spend the most time on the Step Functions row - the JSON-state-machine vs application-code distinction is the one that reframes the cohort's mental model.

**Discussion prompt.** "Step Functions JSON or Java code - which one would your team rather review in a PR?" Most teams will say Java. The win is in the everyday review experience, not just runtime behavior.

### 3. When to keep AWS compute vs replace it (15 min) `[aws]`

**Talking point.** Temporal replaces orchestration *state*, not all compute. Keep AWS compute when:

| Service           | Keep when                                                              | Replace when                                                |
| ----------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------- |
| **Lambda**        | <100ms work, IAM-bound, one-shot                                       | Multi-step coordination, retries, long waits                |
| **Glue Spark**    | Large distributed transforms (>10 GB), Spark-specific operations        | Pure data movement; small batches; orchestration-heavy work |
| **Glue Python**   | Truly tiny scripts (<1 min) with native Glue catalog access            | Anything you'd otherwise write as a Java Activity           |
| **Step Functions**| Already wired, low-change, simple branching                            | Anything requiring human steps, long waits, code review     |

The pattern: **Temporal supervises; AWS executes the heavy lift.** The Glue Spark job stays; the Step Functions JSON disappears.

**Discussion prompt.** "Your Glue job costs $200/run and runs 50 times a day. Is moving it to Temporal Activities a cost win?" Probably not - keep Glue Spark for the transform, wrap it in an Activity that triggers and heartbeats. The savings come from killing Step Functions overhead and consolidating ops, not from re-implementing Spark.

### 4. Wrapping a Glue Spark job as a Temporal Activity (40 min) `[aws]` `[lab]`

**Talking point.** The Glue-as-Activity pattern is the canonical "supervise long-running AWS compute" shape:

1. Activity calls `glue.startJobRun(...)` to get a `runId`.
2. Activity loops: `glue.getJobRun(runId)` -> heartbeat `runId` -> sleep -> check state.
3. On `SUCCEEDED`, return.
4. On `FAILED`/`TIMEOUT`/`STOPPED`, throw `ApplicationFailure` with the Glue error message.

The two correctness requirements:

- **Sleep between polls.** A tight `while(true)` loop will burn through Glue API throttle limits in minutes. 15 seconds is a reasonable default.
- **Heartbeat the `runId`.** On Activity retry, you can read the heartbeat detail and *resume polling* the same Glue run instead of starting a new one.

**Landing example.**

```bash
scripts/show-example.sh 07-aws-containers/glue_activity.java
```

Walk: `runId = glue.startJobRun(...).jobRunId()`, the poll loop with `Activity.getExecutionContext().heartbeat(runId)`, the `Thread.sleep(Duration.ofSeconds(15).toMillis())`, the `ApplicationFailure` mapping.

**Lab.**

```bash
scripts/run-example.sh aws
```

In LocalStack: stub a Glue job that takes 60 seconds to "succeed" (LocalStack's Glue service has limits; for the lab, the runnable mocks `GlueClient`). Watch the Workflow heartbeat in the Web UI; kill the Worker mid-poll; restart it; observe the heartbeat detail recovered.

**Common student errors.**

- Forgetting the sleep -> AWS API throttling errors after a minute or two.
- Returning normally on every Glue state instead of looping -> Workflow thinks it succeeded.
- Not mapping `STOPPED` to a failure -> Workflow runs forever.

### 5. Replacing S3-based checkpointing (25 min) `[aws]` `[lab]`

**Talking point.** S3-as-checkpoint usually exists because no other component remembers state between steps. Temporal's Workflow history *is* the checkpoint, so you can delete those S3 keys.

But: S3 still has a role - as a **payload reference**. Workflow history has a recommended 50 KB per-payload soft limit (hard limit much higher, but you want to stay small). Anything bigger - CSV imports, ML model files, raw event dumps - should live in S3, and the Workflow holds the URI.

**Landing example.**

```bash
scripts/show-example.sh 07-aws-containers/s3_reference_payload.java
```

Walk the `TransformRequest(inputS3Uri, outputPrefix)` -> `TransformResult(outputS3Uri, rowCount)` shape. The Workflow's history holds two URIs and a count. The actual gigabytes never enter the Workflow.

**Discussion prompt.** "When *should* a payload live in Workflow history?" When it's the actual subject of the orchestration: order details, business decisions, control parameters. Not file contents.

**Lab.**

Run the import Workflow against a LocalStack S3 bucket. Inspect the Workflow history - it should contain `s3://` URIs and small metadata, not file bytes.

---

## Break (15 min)

---

## Afternoon - 2 hrs - Running Temporal Workers as Container Workloads

### 6. Temporal Worker as a containerized service - the mental model (15 min) `[containers]`

**Talking point.** A Temporal Worker is **not** an HTTP service. It is a long-lived process that polls Task Queues outbound. There is no inbound traffic, no Service object, no Ingress.

What containers and Kubernetes need to know:

- **Liveness/readiness probes** - need a process-level check (`pgrep`) or a small Actuator endpoint *you add*. They are not handed to you by the SDK.
- **Graceful shutdown** - SIGTERM should drain in-flight Activities, not crash. Set `terminationGracePeriodSeconds` longer than your longest `startToCloseTimeout`, or rely on heartbeat cancellation.
- **No Service or Ingress** - the Worker only talks to the Temporal frontend (outbound gRPC on 7233).

**Discussion prompt.** "What's the right Kubernetes resource for a Worker?" A `Deployment`. Not a `StatefulSet` (no per-pod identity), not a `Job` (long-running), not a `DaemonSet` (one-per-node is wrong sizing).

### 7. Dockerfile for a Java Temporal Worker (25 min) `[containers]` `[lab]`

**Talking point.** Four things a Worker Dockerfile must get right:

1. **Multi-stage build** - separate Maven build from runtime image. Production image carries no Maven, no source.
2. **Container-aware JVM** - `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75`. Without this, the JVM ignores cgroup memory limits and gets OOMKilled.
3. **JRE base image, not JDK** - smaller, fewer CVEs.
4. **No `ENTRYPOINT` script wrappers** - direct `java -jar` so PID 1 receives SIGTERM cleanly.

**Landing example.**

```bash
scripts/show-example.sh 07-aws-containers/Dockerfile
```

Walk each line:

- `FROM maven:3.9-eclipse-temurin-17 AS build` - build stage.
- `RUN mvn -q -DskipTests package` - tests don't run in the image build; they run in CI.
- `FROM eclipse-temurin:17-jre` - runtime stage.
- `ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"` - the container-aware JVM flags.
- `ENTRYPOINT ["java", "-jar", "/app/worker.jar"]` - exec form, no shell.

**Lab.**

```bash
docker build -t temporal-worker:dev examples/runnable/08-aws-containers/
docker run --rm -e TEMPORAL_ADDRESS=host.docker.internal:7233 temporal-worker:dev
```

Have the Worker connect to the local Temporal server and start polling. Kill it with `docker stop`; observe graceful shutdown.

**Common student error.** Using `latest` as the base image tag - rebuilds become non-reproducible. Pin the JRE patch version.

### 8. Kubernetes Deployment for Temporal Workers (25 min) `[containers]` `[lab]`

**Talking point.** Four things a Worker `Deployment` must get right:

1. **Probes that match reality.** The Worker has no HTTP server, so `httpGet: /health` will fail and crashloop the pod. Use an `exec` probe (`pgrep -f worker.jar`) or add a Spring Actuator endpoint and `httpGet` that.
2. **Rolling update aligned to Activity timeouts.** Set `terminationGracePeriodSeconds` >= your longest `startToCloseTimeout`. `maxUnavailable: 0`, `maxSurge: 1` for safe rollouts.
3. **Resource limits matching the JVM heap percent.** If `MaxRAMPercentage=75` and you give the container 2 GiB, the JVM heap caps at 1.5 GiB. Leave headroom for off-heap and GC.
4. **Environment-driven config** - `TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`, `TASK_QUEUE` from `ConfigMap` / `Secret`. No baked-in hostnames.

**Landing example.**

```bash
scripts/show-example.sh 07-aws-containers/worker_deployment.yaml
```

Walk:

- `terminationGracePeriodSeconds: 120` - sized for the longest Activity.
- `exec` probes - the process check that actually matches a no-HTTP service.
- Resource requests vs limits - requests for scheduling, limits for OOMKill protection.

**Lab.**

```bash
kubectl apply -f examples/runnable/08-aws-containers/k8s-worker-deployment.yaml
kubectl rollout status deployment/temporal-transform-worker
kubectl logs -l app=temporal-transform-worker --tail=20
```

Verify the Worker connects to Temporal and starts polling. Then do a rolling deploy with a tag bump and watch the rollout.

**Common student errors.**

- `httpGet` probes that don't match the running container - crashloop with confusing logs.
- `terminationGracePeriodSeconds` shorter than Activity timeout - in-flight Activities get killed; heartbeat triggers retry on another Worker.
- No `resources.limits` - container is not OOM-protected; can take down the node.

### 9. Autoscaling Workers with KEDA (25 min) `[containers]` `[lab]`

**Talking point.** KEDA scales `Deployments` based on external signals. For Temporal, **use the native `type: temporal` scaler** - it polls `DescribeTaskQueue` directly. The older "Prometheus exporter" pattern needs an exporter you'd have to install and there is no built-in `temporal_task_queue_backlog` metric in core Temporal.

Two scaler dimensions to set:

- `queueType` - `WorkflowTaskQueue` or `ActivityTaskQueue`. Activity workers and Workflow workers usually scale on different signals.
- `targetQueueSize` - the backlog at which KEDA starts adding replicas.
- `activationTargetQueueSize` - the backlog at which KEDA wakes the deployment from `minReplicaCount`.

**Landing example.**

```bash
scripts/show-example.sh 07-aws-containers/keda_scaledobject.yaml
```

Walk: `type: temporal`, `endpoint`, `namespace`, `taskQueue`, `queueType: ActivityTaskQueue`, `targetQueueSize: "20"`.

**Lab.**

```bash
kubectl apply -f examples/runnable/08-aws-containers/keda-scaledobject.yaml
# Generate load (POSIX seq, bash-3.2-safe)
for i in $(seq 1 200); do scripts/start-workflow.sh transform "$i"; done
# Watch replicas climb
kubectl get scaledobject,pods -l app=temporal-transform-worker -w
```

**Discussion prompt.** "Why scale Activity Workers and Workflow Workers separately?" Workflow tasks are short CPU bursts; Activity tasks are long I/O. Their backlog signals move independently. Mixing them means one drowns the other's scale signal.

### 10. Running Glue-replacement Activities in containers (15 min) `[aws]` `[containers]`

**Talking point.** A Temporal Worker running Glue-orchestration Activities (the morning's pattern) lives in EKS, polls a `transform` Task Queue, and calls AWS APIs via IAM Roles for Service Accounts (IRSA). It does **not** need to live in the same VPC as Glue - it just needs IAM permissions and outbound access to the Glue endpoint.

Practical setup:

- **IRSA, not access keys** - `eks.amazonaws.com/role-arn` annotation on the ServiceAccount.
- **Outbound to Temporal frontend** - typically a private DNS name into Temporal Cloud or an internal load balancer for self-hosted.
- **Outbound to AWS APIs** - VPC endpoints for Glue, S3, STS if you're going private.

**Discussion prompt.** "Where do you put the Worker if you have one Glue job in `us-east-1` and another in `us-west-2`?" Either one Worker per region (low-latency, simpler IAM) or one Worker across regions (centralized, cross-region AWS API latency). Usually one per region wins.

### 11. Temporal Cloud vs self-hosted on EKS - operational trade-offs (15 min) `[aws]` `[containers]`

**Talking point.** Two viable Temporal deployment models. The choice is rarely technical; it's operational.

| Concern                    | Temporal Cloud                                | Self-hosted on EKS                                  |
| -------------------------- | --------------------------------------------- | --------------------------------------------------- |
| Setup time                 | Hours                                         | Weeks                                               |
| Persistence                | Managed (Cassandra)                           | You run PostgreSQL or Cassandra                     |
| Upgrades                   | Automatic (versions tracked)                  | You schedule and validate                           |
| Multi-region               | Built-in (premium tiers)                      | You design replication                              |
| Cost shape                 | Per-action; scales with usage                 | Fixed infra cost; predictable but not elastic       |
| Network                    | Outbound from your VPC over PrivateLink/mTLS  | All internal; you own DNS, TLS, ingress             |
| Audit / compliance         | SOC2, HIPAA-friendly tiers available          | Your auditor; your evidence                         |
| Search Attributes / advanced visibility | Elasticsearch managed                | You operate Elasticsearch                           |
| Disaster recovery          | Cloud's responsibility                        | Your runbook                                        |

**The decision rule.** Use Cloud unless you have a specific reason not to (data residency, no-cloud policy, existing strong K8s/Cassandra muscle). The hidden cost of self-hosting is *upgrade discipline* - falling behind by 3 minor versions is a painful catch-up.

**Discussion prompt.** "Your security team won't let you send Workflow inputs outside the VPC. Cloud or self-hosted?" Cloud-with-PrivateLink can keep traffic on AWS backbone with no internet hop, but inputs still leave your AWS account. If the policy is "no third party touches the data," self-hosted. Otherwise, Cloud + PrivateLink + payload codec for sensitive fields is the pragmatic answer.

### 12. Lab - containerized end-to-end: S3 trigger -> Temporal Worker -> S3 output (20 min) `[aws]` `[containers]` `[lab]`

**Lab goal.** Run the full Day 6 stack:

1. Drop a file in a LocalStack S3 bucket.
2. An EventBridge-style trigger (mocked locally as an SQS poller or the runnable's start-on-event helper) starts an `ImportWorkflow`.
3. The Workflow runs validate -> transform -> load Activities.
4. Output appears in the destination S3 bucket; Workflow history records URIs only.

```bash
scripts/run-example.sh aws
awslocal s3 cp test-input.csv s3://imports-incoming/
# Watch the Workflow appear in the Web UI
awslocal s3 ls s3://imports-output/
```

**Acceptance.** Workflow shows three Activity completions; output object exists; history contains URIs and a `rowCount`, not file bytes.

**Common errors.**

- LocalStack S3 endpoint not configured in the SDK - SDK tries real S3 and fails with auth error.
- IRSA-style auth doesn't apply locally; use static creds for LocalStack.

---

## Windows PowerShell shortcuts

```powershell
scripts/show-example.ps1 07-aws-containers/aws_mapping.md
scripts/show-example.ps1 07-aws-containers/glue_activity.java
scripts/show-example.ps1 07-aws-containers/s3_reference_payload.java
scripts/show-example.ps1 07-aws-containers/step_functions_before.json
scripts/show-example.ps1 07-aws-containers/step_functions_after_temporal.java
scripts/show-example.ps1 07-aws-containers/Dockerfile
scripts/show-example.ps1 07-aws-containers/worker_deployment.yaml
scripts/show-example.ps1 07-aws-containers/keda_scaledobject.yaml
scripts/run-example.ps1 aws
```

---

## Instructor Notes

- Temporal replaces orchestration state, not all compute. Keep Glue Spark; replace Step Functions JSON.
- Keep Workflow history small. Store large files in S3 and pass URIs. Anything over ~50 KB per payload is a smell.
- **Workers have no inbound traffic.** No Service, no Ingress, no `httpGet` probe by default. Use `exec` probes or add an Actuator endpoint deliberately.
- `terminationGracePeriodSeconds` >= longest `startToCloseTimeout`. Rolling updates respect Activity timeouts or you cause retries.
- **KEDA's native Temporal scaler is the right one.** The Prometheus exporter pattern is older and adds moving parts you don't need.
- Temporal Cloud is the default recommendation for new deployments. Self-host only when you have a specific reason and the K8s/Cassandra discipline.
- Glue Activities + IRSA + VPC endpoints is the production AWS shape. Don't ship static access keys.
