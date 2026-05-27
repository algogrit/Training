class OutcomeProducerActivity implements OutcomeActivities {
  private final KafkaProducer<String, String> producer;

  OutcomeProducerActivity(String bootstrapServers) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    this.producer = new KafkaProducer<>(properties);
  }

  @Override
  public void publishOutcome(String orderId, String outcome) {
    producer.send(new ProducerRecord<>("order-outcomes", orderId, outcome)).join();
  }
}

