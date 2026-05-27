class OutboxActivity {
  void writeOrderAndOutbox(Order order) {
    // One database transaction: business row and outbox row commit together.
    transactionTemplate.execute(
        status -> {
          orderRepository.save(order);
          outboxRepository.save(
              new OutboxMessage(
                  "order-events",
                  order.id(),
                  json.serialize(new OrderAccepted(order.id()))));
          return null;
        });
  }
}

