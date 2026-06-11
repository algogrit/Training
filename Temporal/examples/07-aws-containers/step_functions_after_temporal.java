class ImportWorkflowImpl implements ImportWorkflow {
  private final ImportActivities activities = Workflow.newActivityStub(ImportActivities.class);

  @Override
  public void run(String inputS3Uri) {
    ValidatedFile file = activities.validate(inputS3Uri);
    String transformedUri = activities.transform(file.cleanInputUri());
    LoadResult loadResult = activities.load(transformedUri);
    // Activity method intentionally not named "notify" — that would shadow
    // Object.notify() and confuse callers and reflection-based tooling.
    activities.publishNotification(loadResult);
  }
}
