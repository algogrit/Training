class TimeWorkflow {
  void run() {
    // Replay-safe timer. It records a TimerStarted event and does not block a worker thread.
    Workflow.sleep(Duration.ofHours(6));

    // Replay-safe time source.
    long deadline = Workflow.currentTimeMillis() + Duration.ofDays(1).toMillis();
  }
}

