package training.temporal.aws;

import io.temporal.activity.Activity;
import java.time.Duration;

public class ImportActivitiesImpl implements ImportActivities {

  @Override
  public String validate(String inputS3Uri) {
    sleep(Duration.ofMillis(250));
    Activity.getExecutionContext().heartbeat("validated");
    return inputS3Uri.replace("/incoming/", "/validated/");
  }

  @Override
  public String transform(String validatedS3Uri) {
    sleep(Duration.ofSeconds(1));
    Activity.getExecutionContext().heartbeat("transformed");
    return validatedS3Uri.replace("/validated/", "/transformed/");
  }

  @Override
  public long load(String transformedS3Uri) {
    sleep(Duration.ofMillis(500));
    return Math.abs(transformedS3Uri.hashCode()) % 10_000;
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
