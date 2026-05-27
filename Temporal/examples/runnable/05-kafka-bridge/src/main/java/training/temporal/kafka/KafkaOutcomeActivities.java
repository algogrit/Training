package training.temporal.kafka;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaOutcomeActivities implements OutcomeActivities {
  private final String topic;
  private final Properties properties;

  public KafkaOutcomeActivities(String bootstrapServers, String topic) {
    this.topic = topic;
    this.properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
  }

  @Override
  public void publishOutcome(String orderId, String outcome) {
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
      producer.send(new ProducerRecord<>(topic, orderId, outcome)).get();
    } catch (Exception e) {
      throw new RuntimeException("failed to publish Kafka outcome for " + orderId, e);
    }
  }
}
