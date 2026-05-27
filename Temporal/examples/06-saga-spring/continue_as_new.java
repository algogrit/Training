class SubscriptionWorkflowImpl implements SubscriptionWorkflow {
  @Override
  public void run(String subscriptionId, int eventCount) {
    while (true) {
      Workflow.await(() -> hasNextSubscriptionEvent());
      handleNextSubscriptionEvent();
      eventCount++;

      if (eventCount >= 1000) {
        Workflow.continueAsNew(subscriptionId, 0);
      }
    }
  }
}

