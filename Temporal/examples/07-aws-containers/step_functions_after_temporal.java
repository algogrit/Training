class ImportWorkflowImpl implements ImportWorkflow {
  private final ImportActivities activities = Workflow.newActivityStub(ImportActivities.class);

  @Override
  public void run(String inputS3Uri) {
    ValidatedFile file = activities.validate(inputS3Uri);
    String transformedUri = activities.transform(file.cleanInputUri());
    LoadResult loadResult = activities.load(transformedUri);
    activities.notify(loadResult);
  }
}

