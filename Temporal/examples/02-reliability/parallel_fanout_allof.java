class PartitionFanoutWorkflow {
  private final PartitionActivities activities =
      Workflow.newActivityStub(
          PartitionActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(15)).build());

  int processPartitions(List<Integer> partitions) {
    List<Promise<Integer>> counts =
        partitions.stream()
            .map(partition -> Async.function(activities::processPartition, partition))
            .toList();

    Promise.allOf(counts).get();
    return counts.stream().mapToInt(Promise::get).sum();
  }
}

