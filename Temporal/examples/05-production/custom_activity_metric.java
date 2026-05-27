class InvoiceActivitiesImpl implements InvoiceActivities {
  private final Counter invoiceCounter;

  InvoiceActivitiesImpl(MeterRegistry registry) {
    this.invoiceCounter = Counter.builder("training_invoices_generated_total").register(registry);
  }

  @Override
  public String generateInvoice(String orderId) {
    invoiceCounter.increment();
    return "s3://invoices/" + orderId + ".pdf";
  }
}

