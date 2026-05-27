class KafkaPollActivitiesImpl implements KafkaPollActivities {
  private final KafkaConsumer<String, String> consumer;

  @Override
  public List<String> pollBatch(String topic) {
    consumer.subscribe(List.of(topic));
    List<String> values = new ArrayList<>();

    while (values.size() < 100) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
      for (ConsumerRecord<String, String> record : records) {
        values.add(record.value());
        Activity.getExecutionContext()
            .heartbeat(record.topic() + ":" + record.partition() + ":" + record.offset());
      }
    }

    // Commit only after Activity work succeeds.
    consumer.commitSync();
    return values;
  }
}

