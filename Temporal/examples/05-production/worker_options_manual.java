class ManualWorkerSizing {
  void start(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker =
        factory.newWorker(
            "io-heavy",
            WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(200)
                .setMaxConcurrentWorkflowTaskExecutionSize(20)
                .build());

    worker.registerWorkflowImplementationTypes(IoWorkflowImpl.class);
    worker.registerActivitiesImplementations(new IoActivitiesImpl());
    factory.start();
  }
}

