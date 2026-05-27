class ApprovalWithTimeoutWorkflow {
  boolean waitForApproval(Duration timeout) {
    final boolean[] approved = {false};

    CancellationScope scope =
        Workflow.newCancellationScope(
            () -> {
              Workflow.await(timeout, () -> approved[0]);
              if (!approved[0]) {
                throw ApplicationFailure.newFailure("approval timed out", "ApprovalTimeout");
              }
            });

    scope.run();
    return approved[0];
  }
}

