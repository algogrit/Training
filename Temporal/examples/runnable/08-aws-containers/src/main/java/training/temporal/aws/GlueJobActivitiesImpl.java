package training.temporal.aws;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;
import java.time.Duration;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetJobRunRequest;
import software.amazon.awssdk.services.glue.model.JobRunState;
import software.amazon.awssdk.services.glue.model.StartJobRunRequest;

public class GlueJobActivitiesImpl implements GlueJobActivities {
  private final GlueClient glue;

  public GlueJobActivitiesImpl(GlueClient glue) {
    this.glue = glue;
  }

  @Override
  public String runGlueJob(String jobName, String inputS3Uri) {
    String runId =
        glue.startJobRun(
                StartJobRunRequest.builder()
                    .jobName(jobName)
                    .arguments(java.util.Map.of("--input", inputS3Uri))
                    .build())
            .jobRunId();

    while (true) {
      Activity.getExecutionContext().heartbeat(runId);
      var jobRun =
          glue.getJobRun(GetJobRunRequest.builder().jobName(jobName).runId(runId).build()).jobRun();
      JobRunState state = jobRun.jobRunState();

      if (state == JobRunState.SUCCEEDED) {
        return runId;
      }
      if (state == JobRunState.FAILED || state == JobRunState.TIMEOUT || state == JobRunState.STOPPED) {
        throw ApplicationFailure.newFailure(
            "Glue job " + jobName + " ended as " + state + ": " + jobRun.errorMessage(),
            "GlueJobFailed");
      }

      try {
        Thread.sleep(Duration.ofSeconds(15).toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw ApplicationFailure.newFailure("Interrupted while polling Glue", "GluePollingInterrupted");
      }
    }
  }
}

