class OrderSagaWorkflowImpl implements OrderSagaWorkflow {
  private final OrderActivities activities = Workflow.newActivityStub(OrderActivities.class);

  @Override
  public String process(String orderId) {
    Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
    try {
      String paymentId = activities.authorizePayment(orderId);
      saga.addCompensation(activities::cancelPayment, paymentId);

      String reservationId = activities.reserveInventory(orderId);
      saga.addCompensation(activities::restoreInventory, reservationId);

      activities.ship(orderId);
      return "COMPLETED";
    } catch (RuntimeException failure) {
      saga.compensate();
      activities.sendFailureNotification(orderId, failure.getMessage());
      return "COMPENSATED";
    }
  }
}

