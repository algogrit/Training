package training.temporal.testing;

import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ReminderWorkflowImpl implements ReminderWorkflow {
  @Override
  public String remindAfterOneDay(String message) {
    Workflow.sleep(Duration.ofDays(1));
    return "Reminder: " + message;
  }
}

