package training.temporal.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReminderWorkflowTest {
  @Test
  void skipsWorkflowTime() {
    String taskQueue = "test-reminder";
    try (TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance()) {
      Worker worker = testEnv.newWorker(taskQueue);
      worker.registerWorkflowImplementationTypes(ReminderWorkflowImpl.class);
      testEnv.start();

      WorkflowClient client = testEnv.getWorkflowClient();
      ReminderWorkflow workflow =
          client.newWorkflowStub(
              ReminderWorkflow.class,
              WorkflowOptions.newBuilder().setTaskQueue(taskQueue).build());

      var execution = WorkflowClient.start(workflow::remindAfterOneDay, "ship report");
      testEnv.sleep(Duration.ofDays(1));

      String result = client.newUntypedWorkflowStub(execution.getWorkflowId()).getResult(String.class);
      assertEquals("Reminder: ship report", result);
    }
  }
}

