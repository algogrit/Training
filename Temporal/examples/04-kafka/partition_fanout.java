class PartitionRangeWorkflow {
  private final KafkaPartitionActivities activities =
      Workflow.newActivityStub(
          KafkaPartitionActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(20)).build());

  int processRanges() {
    // Keep fan-out capped for laptops and for predictable Activity pressure.
    List<PartitionRange> ranges =
        List.of(new PartitionRange(0, 3), new PartitionRange(4, 7), new PartitionRange(8, 11));

    List<Promise<Integer>> counts =
        ranges.stream().map(range -> Async.function(activities::processRange, range)).toList();

    Promise.allOf(counts).get();
    return counts.stream().mapToInt(Promise::get).sum();
  }
}

