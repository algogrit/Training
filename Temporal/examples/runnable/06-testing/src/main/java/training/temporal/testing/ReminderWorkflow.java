package training.temporal.testing;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ReminderWorkflow {
  @WorkflowMethod
  String remindAfterOneDay(String message);
}

