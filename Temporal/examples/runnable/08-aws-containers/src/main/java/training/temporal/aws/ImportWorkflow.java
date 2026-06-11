package training.temporal.aws;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ImportWorkflow {
  @WorkflowMethod
  String run(String inputS3Uri);
}
