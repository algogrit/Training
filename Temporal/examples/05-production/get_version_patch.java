class VersionedWorkflow {
  private final PaymentActivities payments = Workflow.newActivityStub(PaymentActivities.class);

  void run(String orderId) {
    int version = Workflow.getVersion("charge-before-reserve", Workflow.DEFAULT_VERSION, 1);

    if (version == Workflow.DEFAULT_VERSION) {
      payments.reserve(orderId);
      payments.charge(orderId);
    } else {
      payments.charge(orderId);
      payments.reserve(orderId);
    }
  }
}

