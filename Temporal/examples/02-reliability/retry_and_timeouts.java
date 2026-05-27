class ActivityOptionsExamples {
  private final ReportActivities reports =
      Workflow.newActivityStub(
          ReportActivities.class,
          ActivityOptions.newBuilder()
              // One attempt must finish inside this window.
              .setStartToCloseTimeout(Duration.ofMinutes(5))
              // The whole retry series must finish inside this window.
              .setScheduleToCloseTimeout(Duration.ofMinutes(30))
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setInitialInterval(Duration.ofSeconds(5))
                      .setBackoffCoefficient(2.0)
                      .setMaximumInterval(Duration.ofMinutes(1))
                      .setMaximumAttempts(6)
                      .build())
              .build());
}

