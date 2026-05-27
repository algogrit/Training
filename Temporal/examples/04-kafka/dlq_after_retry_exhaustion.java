class DlqRoutingWorkflow {
  private final OrderActivities orders =
      Workflow.newActivityStub(
          OrderActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build())
              .build());

  private final DlqActivities dlq =
      Workflow.newActivityStub(
          DlqActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  void process(String orderId) {
    try {
      orders.validate(orderId);
    } catch (ActivityFailure exhausted) {
      dlq.publish(orderId, exhausted.getMessage());
    }
  }
}

