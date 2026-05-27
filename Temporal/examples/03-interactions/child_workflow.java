class ParentOrderWorkflow {
  void process(String orderId) {
    FraudWorkflow fraud =
        Workflow.newChildWorkflowStub(
            FraudWorkflow.class,
            ChildWorkflowOptions.newBuilder().setTaskQueue("fraud").build());

    ShippingWorkflow shipping =
        Workflow.newChildWorkflowStub(
            ShippingWorkflow.class,
            ChildWorkflowOptions.newBuilder().setTaskQueue("shipping").build());

    Promise<String> fraudDecision = Async.function(fraud::check, orderId);
    Promise<String> shippingPlan = Async.function(shipping::plan, orderId);

    Promise.allOf(fraudDecision, shippingPlan).get();
  }
}

