class GlueJobActivitiesImpl implements GlueJobActivities {
  private final GlueClient glue;

  @Override
  public String runGlueJob(String jobName, String inputS3Uri) {
    String runId =
        glue.startJobRun(
                StartJobRunRequest.builder()
                    .jobName(jobName)
                    .arguments(Map.of("--input", inputS3Uri))
                    .build())
            .jobRunId();

    while (true) {
      Activity.getExecutionContext().heartbeat(runId);
      JobRun jobRun =
          glue.getJobRun(GetJobRunRequest.builder().jobName(jobName).runId(runId).build()).jobRun();

      if (jobRun.jobRunState() == JobRunState.SUCCEEDED) {
        return runId;
      }
      if (Set.of(JobRunState.FAILED, JobRunState.TIMEOUT, JobRunState.STOPPED)
          .contains(jobRun.jobRunState())) {
        throw ApplicationFailure.newFailure(jobRun.errorMessage(), "GlueJobFailed");
      }
    }
  }
}

