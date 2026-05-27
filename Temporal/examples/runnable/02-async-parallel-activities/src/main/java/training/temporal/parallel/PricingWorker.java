package training.temporal.parallel;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.List;

public class PricingWorker {
  private static final String TASK_QUEUE = "pricing";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(OrderPricingWorkflowImpl.class);
    worker.registerActivitiesImplementations(new PricingActivitiesImpl());
    factory.start();

    OrderPricingWorkflow workflow =
        client.newWorkflowStub(
            OrderPricingWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    int total = workflow.total(List.of("book", "lamp", "desk"));
    System.out.println("Total price: " + total);
    factory.shutdown();
  }
}

