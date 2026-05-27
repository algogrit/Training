@WorkflowVersioningBehavior(VersioningBehavior.PINNED)
class ShortLivedCheckoutWorkflow implements CheckoutWorkflow {
  @Override
  public void run(String cartId) {
    // Existing executions stay on compatible workers during rollout.
  }
}

@WorkflowVersioningBehavior(VersioningBehavior.AUTO_UPGRADE)
class SubscriptionLifecycleWorkflow implements SubscriptionWorkflow {
  @Override
  public void run(String subscriptionId) {
    // Long-running executions can move to newer compatible worker code.
  }
}

