package com.dulno.gitlab.structure;

import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseComparison;
import com.dulno.core.database.condition.DatabaseCondition;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GitlabDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "gitlab";

  public static GitlabDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("owner", DatabaseDataType.UUID));
    columns.add(DatabaseColumn.create("type", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("hostname", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("application", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("secret", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("accessToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("expiration", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("refreshToken", DatabaseDataType.TEXT));
    return new GitlabDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GitlabDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> insertGitlab(Gitlab gitlab) {
    return insertGitlab(gitlab.id(), gitlab.ownerId(), gitlab.type(),
      gitlab.hostname(), gitlab.applicationId(), gitlab.secret(),
      gitlab.accessToken(), gitlab.expiration(), gitlab.refreshToken());
  }

  public CompletableFuture<Void> insertGitlab(
    UUID id, UUID ownerId, GitlabType type, String hostname,
    String applicationId, String secret, String accessToken,
    long expiration, String refreshToken
  ) {
    return insert(DatabaseRow.of(id, ownerId, type.toString(),
      hostname, applicationId, secret, accessToken, expiration, refreshToken));
  }

  public CompletableFuture<UUID> generateAvailableGitlabId() {
    var futureResponse = new CompletableFuture<UUID>();
    var id = UUID.randomUUID();
    gitlabExists(id).thenApply(exists -> exists ?
      generateAvailableGitlabId().thenApply(futureResponse::complete) :
      CompletableFuture.completedFuture(futureResponse.complete(id)));
    return futureResponse;
  }

  public void updateGitlabAccess(
    Gitlab gitlab, String accessToken, long expiration, String refreshToken
  ) {
    gitlab.updateAccessToken(accessToken);
    gitlab.updateExpiration(expiration);
    gitlab.updateRefreshToken(refreshToken);
    updateGitlab(gitlab);
  }

  private void updateGitlab(Gitlab gitlab) {
    update(gitlab.id(), DatabaseRow.of(gitlab.id(), gitlab.ownerId(),
      gitlab.type().toString(), gitlab.hostname(), gitlab.applicationId(),
      gitlab.secret(), gitlab.accessToken(), gitlab.expiration(),
      gitlab.refreshToken()));
  }

  public void deleteGitlab(UUID id) {
    delete(id);
  }

  public CompletableFuture<Boolean> gitlabExists(UUID id) {
    return exists(id);
  }

  public CompletableFuture<Boolean> gitlabExistsByOwner(UUID ownerId) {
    var condition = DatabaseCondition.of(
      DatabaseComparison.create("owner", ownerId));
    return exists(condition);
  }

  public CompletableFuture<Boolean> gitlabExists(UUID ownerId, String hostname) {
    return exists(DatabaseCondition.of(DatabaseCondition.Filtering.ALLOWED,
      DatabaseComparison.create("owner", ownerId),
      DatabaseComparison.create("hostname", hostname)));
  }

  public CompletableFuture<Gitlab> findGitlab(UUID id) {
    return selectRow(id).thenApply(Gitlab::of);
  }

  public CompletableFuture<List<Gitlab>> findGitlabsOfOwner(UUID ownerId) {
    var condition = DatabaseCondition.of(
      DatabaseComparison.create("owner", ownerId));
    return selectRows(condition).thenApply(rows ->
      rows.stream().map(Gitlab::of).toList());
  }
}
