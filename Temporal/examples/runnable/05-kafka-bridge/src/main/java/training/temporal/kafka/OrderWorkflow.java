package training.temporal.kafka;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {
  @WorkflowMethod
  String run(String orderId);

  @SignalMethod
  void orderEvent(String payload);
}

