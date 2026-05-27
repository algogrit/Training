record TransformRequest(String inputS3Uri, String outputPrefix) {}

record TransformResult(String outputS3Uri, long rowCount) {}

class TransformWorkflow {
  private final TransformActivities activities = Workflow.newActivityStub(TransformActivities.class);

  TransformResult run(TransformRequest request) {
    // Keep Workflow history small. Pass S3 references, not file contents.
    String outputUri = activities.transform(request.inputS3Uri(), request.outputPrefix());
    long rowCount = activities.countRows(outputUri);
    return new TransformResult(outputUri, rowCount);
  }
}

