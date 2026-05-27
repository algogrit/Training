package training.temporal.parallel;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface OrderPricingWorkflow {
  @WorkflowMethod
  int total(List<String> skus);
}

