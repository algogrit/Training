package training.temporal.aws;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ImportActivities {
  String validate(String inputS3Uri);

  String transform(String validatedS3Uri);

  long load(String transformedS3Uri);
}
