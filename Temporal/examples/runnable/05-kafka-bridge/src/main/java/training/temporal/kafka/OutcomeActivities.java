package training.temporal.kafka;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface OutcomeActivities {
  @ActivityMethod
  void publishOutcome(String orderId, String outcome);
}

