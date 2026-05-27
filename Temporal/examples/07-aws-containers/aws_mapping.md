# AWS to Temporal Examples

| Existing AWS Shape | Temporal Shape |
|---|---|
| EventBridge rule invokes Lambda that starts Step Functions | Consumer or Lambda starts/signals a Workflow |
| Step Functions branches through JSON states | Workflow branches through Java code |
| Glue Python shell job writes checkpoints to S3 | Activity returns result; Workflow history records whether it ran |
| Glue Spark job does heavy distributed transform | Activity starts Glue job and heartbeats run ID while polling |
| CloudWatch retry on Lambda | Activity `RetryOptions` with typed failures |
| S3 handoff file between Lambdas | Activity returns S3 URI for large payloads; Workflow owns sequencing |

