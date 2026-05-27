class PartialFailureWorkflow {
  private final PartitionActivities activities =
      Workflow.newActivityStub(
          PartitionActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(15)).build());

  Map<Integer, String> process(List<Integer> partitions) {
    Map<Integer, Promise<String>> futures = new LinkedHashMap<>();
    for (int partition : partitions) {
      futures.put(partition, Async.function(activities::processWithStatus, partition));
    }

    Map<Integer, String> result = new LinkedHashMap<>();
    for (var entry : futures.entrySet()) {
      try {
        result.put(entry.getKey(), entry.getValue().get());
      } catch (ActivityFailure failure) {
        result.put(entry.getKey(), "FAILED: " + failure.getMessage());
      }
    }
    return result;
  }
}

