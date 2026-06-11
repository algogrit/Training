class OrderApi {
  // Sync request/response over a saga. startUpdateWithStart creates the
  // workflow if it does not exist yet and applies the update in one round
  // trip, so the caller can wait on a result without a separate start RPC.
  String submitOrder(WorkflowClient client, OrderRequest request) {
    OrderSagaWorkflow workflow =
        client.newWorkflowStub(
            OrderSagaWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("order-" + request.orderId())
                .setTaskQueue("orders")
                .build());

    WithStartWorkflowOperation<String> startOperation =
        WithStartWorkflowOperation.newBuilder(workflow::process)
            .setArguments(request.orderId())
            .build();

    WorkflowUpdateHandle<String> update =
        client.startUpdateWithStart(
            startOperation,
            "submit",
            WorkflowUpdateStage.COMPLETED,
            String.class,
            request);

    return update.getResult();
  }
}
