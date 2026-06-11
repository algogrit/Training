package training.temporal.aws;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public final class WorkerMain {

  private WorkerMain() {}

  public static void main(String[] args) {
    String target = System.getenv().getOrDefault("TEMPORAL_ADDRESS", "127.0.0.1:7233");
    String namespace = System.getenv().getOrDefault("TEMPORAL_NAMESPACE", "default");
    String taskQueue = System.getenv().getOrDefault("TASK_QUEUE", "transform");

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());

    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            io.temporal.client.WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(ImportWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ImportActivitiesImpl());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down worker...");
      factory.shutdown();
    }));

    factory.start();
    System.out.printf(
        "Worker started. target=%s namespace=%s taskQueue=%s%n", target, namespace, taskQueue);
  }
}
