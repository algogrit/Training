package training.temporal.schedules;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DailyReportWorkflow {
  @WorkflowMethod
  void run(String reportName);
}

