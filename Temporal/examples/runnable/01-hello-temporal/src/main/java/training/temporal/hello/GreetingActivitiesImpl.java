package training.temporal.hello;

public class GreetingActivitiesImpl implements GreetingActivities {
  @Override
  public String composeGreeting(String name) {
    return "Hello, " + name + " from a Temporal Activity";
  }
}

