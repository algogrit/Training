@WorkflowInterface
interface CartWorkflow {
  @WorkflowMethod
  String checkout(String cartId);

  @UpdateMethod
  int addItem(String sku, int quantity);

  @UpdateValidatorMethod(updateName = "addItem")
  void validateAddItem(String sku, int quantity);
}

class CartClient {
  void update(WorkflowClient client, String workflowId) {
    WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
    WorkflowUpdateHandle<Integer> handle =
        stub.startUpdate(
            "addItem",
            WorkflowUpdateStage.COMPLETED,
            Integer.class,
            "book",
            2);

    int itemCount = handle.getResult();
  }
}

