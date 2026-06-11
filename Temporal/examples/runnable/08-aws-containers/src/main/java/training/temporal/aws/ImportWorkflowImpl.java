package training.temporal.aws;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ImportWorkflowImpl implements ImportWorkflow {

  private final ImportActivities activities =
      Workflow.newActivityStub(
          ImportActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofMinutes(2))
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setInitialInterval(Duration.ofSeconds(1))
                      .setMaximumAttempts(3)
                      .build())
              .build());

  @Override
  public String run(String inputS3Uri) {
    String validatedUri = activities.validate(inputS3Uri);
    String transformedUri = activities.transform(validatedUri);
    long rowCount = activities.load(transformedUri);
    return transformedUri + "?rows=" + rowCount;
  }
}
