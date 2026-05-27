// The same shape as the Airflow DAG, expressed as durable application code.
@WorkflowInterface
public interface OrdersWorkflow {
  @WorkflowMethod
  void run(String batchDate);
}

@ActivityInterface
public interface OrdersActivities {
  String extract(String batchDate);

  String transform(String rawUri);

  void load(String cleanUri);
}

public class OrdersWorkflowImpl implements OrdersWorkflow {
  private final OrdersActivities activities =
      Workflow.newActivityStub(
          OrdersActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).build());

  @Override
  public void run(String batchDate) {
    String rawUri = activities.extract(batchDate);
    String cleanUri = activities.transform(rawUri);
    activities.load(cleanUri);
  }
}

