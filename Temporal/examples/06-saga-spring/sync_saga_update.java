class OrderApi {
  String submitOrder(WorkflowClient client, OrderRequest request) {
    WorkflowStub stub = client.newUntypedWorkflowStub("order-" + request.orderId());

    WorkflowUpdateHandle<String> update =
        stub.startUpdate(
            "submit",
            WorkflowUpdateStage.COMPLETED,
            String.class,
            request);

    return update.getResult();
  }
}

