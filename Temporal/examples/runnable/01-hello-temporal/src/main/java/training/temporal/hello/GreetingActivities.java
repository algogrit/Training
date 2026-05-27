package training.temporal.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GreetingActivities {
  @ActivityMethod
  String composeGreeting(String name);
}

