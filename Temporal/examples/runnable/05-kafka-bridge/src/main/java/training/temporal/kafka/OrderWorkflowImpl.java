package training.temporal.kafka;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class OrderWorkflowImpl implements OrderWorkflow {
  private final OutcomeActivities activities =
      Workflow.newActivityStub(
          OutcomeActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  private String payload;

  @Override
  public String run(String orderId) {
    Workflow.await(() -> payload != null);
    String outcome = "accepted:" + orderId;
    activities.publishOutcome(orderId, outcome);
    return outcome;
  }

  @Override
  public void orderEvent(String payload) {
    this.payload = payload;
  }
}

