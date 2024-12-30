package com.dulno.gitlab.trigger;

import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseCondition;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TriggerGitlabDatabaseTable extends DatabaseTable {
  public static TriggerGitlabDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String tableName
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID,
      DatabaseColumn.Type.PARTITION_KEY));
    columns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT,
      DatabaseColumn.Type.CLUSTERING_KEY));
    columns.add(DatabaseColumn.create("webhookSecret", DatabaseDataType.TEXT,
      DatabaseColumn.Type.CLUSTERING_KEY));
    columns.add(DatabaseColumn.create("trigger", DatabaseDataType.UUID,
      DatabaseColumn.Type.CLUSTERING_KEY));
    columns.add(DatabaseColumn.create("ownerId", DatabaseDataType.UUID));
    columns.add(DatabaseColumn.create("webhookId", DatabaseDataType.TEXT));
    return new TriggerGitlabDatabaseTable(connection, keyspace, tableName, columns);
  }

  private TriggerGitlabDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void initialize() {
    createIfNotExists();
    createIndexIfNotExists("trigger");
  }

  public CompletableFuture<Void> insertContent(
    UUID triggerId, UUID ownerId, UUID gitlabId, String projectId, String webhookId,
    String webhookSecret
  ) {
    return insert(DatabaseRow.of(gitlabId, projectId, webhookSecret, triggerId,
      ownerId, webhookId));
  }

  public CompletableFuture<Void> deleteContent(UUID triggerId) {
    return findContent(triggerId).thenAccept(content ->
      delete(DatabaseCondition.of("trigger", triggerId, "gitlabId",
        content.findCell(0).uuidValue(), "projectId",
        content.findCell(1).stringValue(), "webhookSecret",
        content.findCell(2).stringValue())));
  }

  public CompletableFuture<DatabaseRow> findContent(UUID triggerId) {
    return selectRow(DatabaseCondition.of("trigger", triggerId));
  }

  public CompletableFuture<List<DatabaseRow>> findContentByCondition(
    DatabaseCondition condition
  ) {
    return selectRows(condition);
  }
}