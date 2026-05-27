class ReplaySafeWorkflow implements OrdersWorkflow {
  private final OrdersActivities activities =
      Workflow.newActivityStub(
          OrdersActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).build());

  @Override
  public void run(String batchDate) {
    // Temporal records these deterministic decisions in Workflow history.
    long now = Workflow.currentTimeMillis();
    int shard = Workflow.newRandom().nextInt(10);

    // I/O belongs in Activities because Activity results are recorded in history.
    activities.load("s3://bucket/clean/" + batchDate + "/shard=" + shard + "?ts=" + now);
  }
}

