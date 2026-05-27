class UpdateWithStartExample {
  void addItemOrCreateCart(WorkflowClient client) {
    WithStartWorkflowOperation<String> startOperation =
        WithStartWorkflowOperation.newBuilder(CartWorkflow::checkout)
            .setWorkflowId("cart-1001")
            .setTaskQueue("carts")
            .setArguments("cart-1001")
            .build();

    WorkflowUpdateHandle<Integer> update =
        client.startUpdateWithStart(
            startOperation,
            "addItem",
            WorkflowUpdateStage.COMPLETED,
            Integer.class,
            "lamp",
            1);

    int itemCount = update.getResult();
  }
}

