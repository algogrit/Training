package training.temporal.parallel;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PricingActivities {
  @ActivityMethod
  int price(String sku);
}

