@Component
class OrderKafkaListener {
  private final WorkflowClient client;

  OrderKafkaListener(WorkflowClient client) {
    this.client = client;
  }

  // signalWithStart keeps Kafka redeliveries idempotent: the workflow is
  // started once per orderId, and later events for the same key signal the
  // already-running execution instead of crashing.
  @KafkaListener(topics = "orders")
  void onOrder(OrderRequest request) {
    OrderSagaWorkflow workflow =
        client.newWorkflowStub(
            OrderSagaWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("order-" + request.orderId())
                .setTaskQueue("orders")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(workflow::process, request.orderId());
    batch.add(workflow::onUpdate, request);
    client.signalWithStart(batch);
  }
}
