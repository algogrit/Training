class VirtualThreadWorker {
  void start(WorkflowClient client) {
    WorkerFactoryOptions factoryOptions =
        WorkerFactoryOptions.newBuilder().setUsingVirtualWorkflowThreads(true).build();

    WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);
    Worker worker =
        factory.newWorker(
            "high-concurrency-activities",
            WorkerOptions.newBuilder().setUsingVirtualThreads(true).build());

    factory.start();
  }
}

