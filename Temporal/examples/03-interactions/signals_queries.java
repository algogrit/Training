@WorkflowInterface
interface HumanApprovalWorkflow {
  @WorkflowMethod
  String run(String requestId);

  @SignalMethod
  void approve(String approver);

  @QueryMethod
  String currentState();
}

class HumanApprovalWorkflowImpl implements HumanApprovalWorkflow {
  private String state = "WAITING";

  @Override
  public String run(String requestId) {
    Workflow.await(() -> state.startsWith("APPROVED"));
    return state;
  }

  @Override
  public void approve(String approver) {
    state = "APPROVED by " + approver;
  }

  @Override
  public String currentState() {
    return state;
  }
}

