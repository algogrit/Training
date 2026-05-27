class TimeoutClient {
  void start(WorkflowClient client) {
    OrdersWorkflow workflow =
        client.newWorkflowStub(
            OrdersWorkflow.class,
            WorkflowOptions.newBuilder()
                // Maximum lifetime across continue-as-new runs.
                .setWorkflowExecutionTimeout(Duration.ofDays(7))
                // Maximum lifetime for this individual run.
                .setWorkflowRunTimeout(Duration.ofHours(12))
                .setTaskQueue("orders")
                .build());

    WorkflowClient.start(workflow::run, "2026-05-27");
  }
}

