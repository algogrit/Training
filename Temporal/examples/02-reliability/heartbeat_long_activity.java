class ExportActivitiesImpl implements ExportActivities {
  @Override
  public String exportLargeTable(String tableName) {
    for (int page = 0; page < 1000; page++) {
      try {
        exportPage(tableName, page);
        Activity.getExecutionContext().heartbeat(page);
      } catch (ActivityCanceledException | ActivityPausedException stop) {
        cleanupPartialExport(tableName, page);
        throw stop;
      }
    }
    return "s3://exports/" + tableName;
  }
}

