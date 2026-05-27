package training.temporal.saga;

public class OrderActivitiesImpl implements OrderActivities {
  @Override
  public String authorizePayment(String orderId) {
    return "payment-" + orderId;
  }

  @Override
  public String reserveInventory(String orderId) {
    return "reservation-" + orderId;
  }

  @Override
  public void ship(String orderId) {
    if (orderId.endsWith("FAIL")) {
      throw new IllegalStateException("shipping label service failed");
    }
  }

  @Override
  public void cancelPayment(String paymentId) {
    System.out.println("cancelled " + paymentId);
  }

  @Override
  public void restoreInventory(String reservationId) {
    System.out.println("restored " + reservationId);
  }

  @Override
  public void sendFailureNotification(String orderId, String reason) {
    System.out.println("order " + orderId + " failed: " + reason);
  }
}

