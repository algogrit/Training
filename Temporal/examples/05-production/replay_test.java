class ReplayHistoryTest {
  @Test
  void replaysProductionHistory() throws Exception {
    WorkflowReplayer.replayWorkflowExecutionFromResource(
        "histories/order-1001.json",
        OrderSagaWorkflowImpl.class);
  }
}

