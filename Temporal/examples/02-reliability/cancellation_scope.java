class CancellableExportWorkflow {
  private final ExportActivities exports =
      Workflow.newActivityStub(
          ExportActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofHours(2))
              .setHeartbeatTimeout(Duration.ofSeconds(30))
              .build());

  // Start a long-running export inside a CancellationScope, then race it
  // against a deadline. When the deadline wins, scope.cancel() propagates
  // a cancellation request to the Activity — its next heartbeat throws
  // ActivityCanceledException, letting it clean up partial work.
  String exportWithDeadline(String tableName, Duration deadline) {
    CompletablePromise<String> result = Workflow.newPromise();

    CancellationScope scope =
        Workflow.newCancellationScope(
            () -> result.completeFrom(Async.function(exports::exportLargeTable, tableName)));
    scope.run();

    boolean finishedInTime = Workflow.await(deadline, result::isCompleted);
    if (!finishedInTime) {
      scope.cancel("export deadline exceeded");
      throw ApplicationFailure.newFailure("export timed out", "ExportTimeout");
    }
    return result.get();
  }
}
