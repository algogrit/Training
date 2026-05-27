class ResourceBasedWorkerSizing {
  void start(WorkflowClient client) {
    ResourceBasedTuner tuner =
        ResourceBasedTuner.newBuilder()
            .setControllerOptions(
                ResourceBasedControllerOptions.newBuilder()
                    .setTargetMemoryUsage(0.75)
                    .setTargetCpuUsage(0.80)
                    .build())
            .build();

    Worker worker =
        WorkerFactory.newInstance(client)
            .newWorker("payments", WorkerOptions.newBuilder().setWorkerTuner(tuner).build());
  }
}

