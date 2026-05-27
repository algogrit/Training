class MetricsClient {
  WorkflowClient createClient() {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    Scope scope =
        new RootScopeBuilder()
            .reporter(new MicrometerClientStatsReporter(registry))
            .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build());

    return WorkflowClient.newInstance(service);
  }
}

