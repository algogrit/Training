@Component
class OrderKafkaListener {
  private final WorkflowClient client;

  OrderKafkaListener(WorkflowClient client) {
    this.client = client;
  }

  @KafkaListener(topics = "orders")
  void onOrder(OrderRequest request) {
    OrderSagaWorkflow workflow =
        client.newWorkflowStub(
            OrderSagaWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("order-" + request.orderId())
                .setTaskQueue("orders")
                .build());

    WorkflowClient.start(workflow::process, request.orderId());
  }
}

