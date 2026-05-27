class AsyncOrderApi {
  void submitOrder(WorkflowClient client, OrderRequest request) {
    OrderSagaWorkflow workflow =
        client.newWorkflowStub(
            OrderSagaWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("order-" + request.orderId())
                .setTaskQueue("orders")
                .build());

    WorkflowClient.start(workflow::run);
    workflow.submit(request);
  }
}

