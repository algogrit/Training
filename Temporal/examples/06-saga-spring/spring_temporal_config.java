@Configuration
class TemporalConfig {
  @Bean
  WorkflowServiceStubs workflowServiceStubs() {
    return WorkflowServiceStubs.newLocalServiceStubs();
  }

  @Bean
  WorkflowClient workflowClient(WorkflowServiceStubs service) {
    return WorkflowClient.newInstance(service);
  }

  @Bean(initMethod = "start", destroyMethod = "shutdown")
  WorkerFactory workerFactory(WorkflowClient client, OrderActivities activities) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker("orders");
    worker.registerWorkflowImplementationTypes(OrderSagaWorkflowImpl.class);
    worker.registerActivitiesImplementations(activities);
    return factory;
  }
}

