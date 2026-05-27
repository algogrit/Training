package training.temporal.approval;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class ApprovalWorker {
  private static final String TASK_QUEUE = "approval";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
    factory.start();

    ApprovalWorkflow workflow =
        client.newWorkflowStub(
            ApprovalWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId("approval-1001")
                .build());

    WorkflowClient.start(workflow::run, "PO-1001");
    System.out.println(workflow.status());
    System.out.println(workflow.changeNote("expedite before close of business"));
    workflow.approve("manager@example.com");
    System.out.println(WorkflowStub.fromTyped(workflow).getResult(String.class));

    factory.shutdown();
  }
}

