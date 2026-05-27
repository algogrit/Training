package training.temporal.parallel;

import io.temporal.activity.Activity;
import java.util.Map;

public class PricingActivitiesImpl implements PricingActivities {
  private static final Map<String, Integer> PRICES = Map.of("book", 30, "lamp", 75, "desk", 250);

  @Override
  public int price(String sku) {
    Activity.getExecutionContext().heartbeat("pricing " + sku);
    return PRICES.getOrDefault(sku, 10);
  }
}

