package training.temporal.parallel;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;

public class OrderPricingWorkflowImpl implements OrderPricingWorkflow {
  private final PricingActivities activities =
      Workflow.newActivityStub(
          PricingActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .setRetryOptions(
                  RetryOptions.newBuilder()
                      .setInitialInterval(Duration.ofSeconds(1))
                      .setMaximumAttempts(3)
                      .build())
              .build());

  @Override
  public int total(List<String> skus) {
    List<Promise<Integer>> prices =
        skus.stream().map(sku -> Async.function(activities::price, sku)).toList();

    Promise.allOf(prices).get();
    return prices.stream().mapToInt(Promise::get).sum();
  }
}

