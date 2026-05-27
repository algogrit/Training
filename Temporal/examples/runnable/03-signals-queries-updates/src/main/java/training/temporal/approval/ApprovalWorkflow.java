package training.temporal.approval;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ApprovalWorkflow {
  @WorkflowMethod
  String run(String requestId);

  @SignalMethod
  void approve(String approver);

  @SignalMethod
  void reject(String reason);

  @QueryMethod
  String status();

  @UpdateMethod
  String changeNote(String note);

  @UpdateValidatorMethod(updateName = "changeNote")
  void validateNote(String note);
}

