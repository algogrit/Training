class BadReplayWorkflow implements OrdersWorkflow {
  @Override
  public void run(String batchDate) {
    // Bad: replay will re-run Workflow code. This value may change during replay.
    long now = System.currentTimeMillis();

    // Bad: this random value is not recorded in Workflow history.
    int shard = new Random().nextInt(10);

    // Bad: direct I/O in Workflow code can run again during replay.
    Files.writeString(Path.of("/tmp/workflow.log"), now + ":" + shard);
  }
}

