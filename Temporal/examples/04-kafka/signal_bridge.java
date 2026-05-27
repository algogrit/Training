class SignalBridge {
  void onKafkaRecord(WorkflowClient client, ConsumerRecord<String, String> record) {
    String workflowId = "order-" + record.key();

    OrderWorkflow workflow =
        client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("orders")
                .build());

    WorkflowClient.start(workflow::run, record.key());
    workflow.orderEvent(record.value());
  }
}

