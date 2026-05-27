package training.temporal.saga;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface OrderActivities {
  @ActivityMethod
  String authorizePayment(String orderId);

  @ActivityMethod
  String reserveInventory(String orderId);

  @ActivityMethod
  void ship(String orderId);

  @ActivityMethod
  void cancelPayment(String paymentId);

  @ActivityMethod
  void restoreInventory(String reservationId);

  @ActivityMethod
  void sendFailureNotification(String orderId, String reason);
}

