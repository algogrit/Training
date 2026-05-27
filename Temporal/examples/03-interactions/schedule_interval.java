class IntervalScheduleExample {
  void createHourlySchedule(ScheduleClient schedules) {
    Schedule schedule =
        Schedule.newBuilder()
            .setAction(
                ScheduleActionStartWorkflow.newBuilder()
                    .setWorkflowType(OrdersWorkflow.class)
                    .setOptions(WorkflowOptions.newBuilder().setTaskQueue("orders").build())
                    .setArguments("hourly")
                    .build())
            .setSpec(
                ScheduleSpec.newBuilder()
                    .setIntervals(List.of(new ScheduleIntervalSpec(Duration.ofHours(1))))
                    .setJitter(Duration.ofMinutes(5))
                    .build())
            .setPolicy(
                SchedulePolicy.newBuilder()
                    .setOverlap(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_BUFFER_ONE)
                    .build())
            .build();

    schedules.createSchedule("hourly-orders", schedule, ScheduleOptions.newBuilder().build());
  }
}

