class SignalBridge {
  // signalWithStart: starts the Workflow on the first event for a key, and
  // signals the existing execution for every event after that. Plain start()
  // would throw WorkflowExecutionAlreadyStarted on the second event.
  void onKafkaRecord(WorkflowClient client, ConsumerRecord<String, String> record) {
    String workflowId = "order-" + record.key();

    OrderWorkflow workflow =
        client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("orders")
                .build());

    BatchRequest batch = client.newSignalWithStartRequest();
    batch.add(workflow::run, record.key());
    batch.add(workflow::orderEvent, record.value());
    client.signalWithStart(batch);
  }
}
