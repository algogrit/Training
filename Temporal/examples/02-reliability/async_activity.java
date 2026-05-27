class AsyncWorkflow implements OrdersWorkflow {
  private final OrdersActivities activities =
      Workflow.newActivityStub(
          OrdersActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build());

  @Override
  public void run(String batchDate) {
    Promise<String> rawUri = Async.function(activities::extract, batchDate);
    Promise<String> auditUri = Async.function(activities::extract, batchDate + "-audit");

    String cleanUri = activities.transform(rawUri.get());
    String cleanAuditUri = activities.transform(auditUri.get());

    activities.load(cleanUri);
    activities.load(cleanAuditUri);
  }
}

