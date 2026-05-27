// DAG -> Workflow: durable orchestration state and decisions.
@WorkflowInterface
interface LoanWorkflow {
  @WorkflowMethod
  String apply(String applicationId);
}

// Operator -> Activity: unreliable work with retries, timeouts, and side effects.
@ActivityInterface
interface LoanActivities {
  int pullCreditScore(String applicationId);

  void notifyApplicant(String applicationId, String decision);
}

// Executor -> Worker: long-running process polling a Task Queue.
class WorkerBootstrap {
  void start(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("loan-decisions");
    worker.registerWorkflowImplementationTypes(LoanWorkflowImpl.class);
    worker.registerActivitiesImplementations(new LoanActivitiesImpl());
    factory.start();
  }
}

