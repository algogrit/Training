package training.temporal.aws;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GlueJobActivities {
  @ActivityMethod
  String runGlueJob(String jobName, String inputS3Uri);
}

