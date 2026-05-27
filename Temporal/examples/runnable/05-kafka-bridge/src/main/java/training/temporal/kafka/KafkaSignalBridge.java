package training.temporal.kafka;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaSignalBridge implements Runnable {
  private final WorkflowClient client;
  private final String taskQueue;
  private final String topic;
  private final Properties properties;

  public KafkaSignalBridge(
      WorkflowClient client, String taskQueue, String bootstrapServers, String topic) {
    this.client = client;
    this.taskQueue = taskQueue;
    this.topic = topic;
    this.properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "temporal-order-bridge");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
  }

  @Override
  public void run() {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
      consumer.subscribe(List.of(topic));
      while (!Thread.currentThread().isInterrupted()) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(1))) {
          String orderId = record.key();
          OrderWorkflow workflow =
              client.newWorkflowStub(
                  OrderWorkflow.class,
                  WorkflowOptions.newBuilder()
                      .setWorkflowId("order-" + orderId)
                      .setTaskQueue(taskQueue)
                      .build());
          WorkflowClient.start(workflow::run, orderId);
          workflow.orderEvent(record.value());
          consumer.commitSync();
        }
      }
    }
  }
}

