package training.temporal.schedules;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleCalendarSpec;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.SchedulePolicy;
import io.temporal.client.schedules.ScheduleRange;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.schedules.ScheduleState;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.api.enums.v1.ScheduleOverlapPolicy;
import java.util.List;

public class CreateSchedule {
  public static void main(String[] args) {
    ScheduleClient scheduleClient =
        ScheduleClient.newInstance(WorkflowServiceStubs.newLocalServiceStubs());

    Schedule schedule =
        Schedule.newBuilder()
            .setAction(
                ScheduleActionStartWorkflow.newBuilder()
                    .setWorkflowType(DailyReportWorkflow.class)
                    .setArguments("daily-sales")
                    .setOptions(
                        WorkflowOptions.newBuilder()
                            .setTaskQueue("reports")
                            .setWorkflowId("daily-sales-report")
                            .build())
                    .build())
            .setSpec(
                ScheduleSpec.newBuilder()
                    .setCalendars(
                        List.of(
                            ScheduleCalendarSpec.newBuilder()
                                .setHour(List.of(new ScheduleRange(9)))
                                .setMinutes(List.of(new ScheduleRange(0)))
                                .build()))
                    .build())
            .setPolicy(
                SchedulePolicy.newBuilder()
                    .setOverlap(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_SKIP)
                    .build())
            .setState(ScheduleState.newBuilder().setNote("Airflow daily DAG replacement").build())
            .build();

    scheduleClient.createSchedule("daily-sales-report-schedule", schedule, ScheduleOptions.newBuilder().build());
  }
}
