class AsyncOrderApi {
  // Fire-and-forget saga submission. signalWithStart guarantees the workflow
  // exists before the signal is delivered — without it, the signal would race
  // the start RPC and could land on a not-yet-created execution.
  void submitOrder(WorkflowClient client, OrderRequest request) {
    OrderSagaWorkflow workflow =
        client.newWorkflowStub(
            OrderSagaWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("order-" + request.orderId())
                .setTaskQueue("orders")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(workflow::run);
    batch.add(workflow::submit, request);
    client.signalWithStart(batch);
  }
}
