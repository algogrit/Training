package training.temporal.saga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderSagaWorkflow {
  @WorkflowMethod
  String process(String orderId);
}

