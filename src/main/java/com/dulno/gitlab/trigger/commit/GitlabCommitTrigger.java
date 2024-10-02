package com.dulno.gitlab.trigger.commit;

import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseCondition;
import com.dulno.core.trigger.Trigger;
import com.dulno.core.trigger.TriggerContentDatabaseTable;
import com.dulno.core.trigger.TriggerInformation;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.core.workflow.component.input.InputComponentVariable;
import com.dulno.core.workflow.component.output.OutputComponentVariable;
import com.dulno.gitlab.structure.GitlabWebhookFactory;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class GitlabCommitTrigger implements Trigger {
  public static GitlabCommitTrigger create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabWebhookFactory gitlabWebhookFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("webhookId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("webhookSecret", DatabaseDataType.TEXT));
    return new GitlabCommitTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory,
      TriggerContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_commit", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final TriggerContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-commit-trigger";
  }

  @Override
  public TriggerInformation information() {
    return TriggerInformation.builder()
      .withName("gitlab.trigger.commit.name")
      .withDescription("gitlab.trigger.commit.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.commit.input.gitlab.name",
        "gitlabIdentifier", "gitlab.trigger.commit.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.commit.input.project.name",
        "projectIdentifier", "gitlab.trigger.commit.input.project.description", projectComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.commit.output.identifier", "commitIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.commit.output.reference", "commitReference"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.commit.output.message", "commitMessage"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.commit.output.author.name", "commitAuthorName"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.commit.output.author.email", "commitAuthorEmail"))
      .build();
  }

  @Override
  public void initialize() {
    contentDatabaseTable.createIfNotExists();
    contentDatabaseTable.createIndexIfNotExists("gitlabId");
    contentDatabaseTable.createIndexIfNotExists("projectId");
    contentDatabaseTable.createIndexIfNotExists("webhookSecret");
  }

  @Override
  public CompletableFuture<Void> insert(UUID triggerId, Map<String, Object> content) {
    var gitlabId = UUID.fromString((String) content.get("gitlabIdentifier"));
    var projectId = (String) content.get("projectIdentifier");
    var webhookSecret = UUID.randomUUID().toString();
    return gitlabWebhookFactory.build(gitlabId, projectId)
      .create("push_events", webhookSecret)
      .thenCompose(webhookId -> contentDatabaseTable.insertContent(triggerId,
        DatabaseRow.of(gitlabId, projectId, webhookId, webhookSecret)));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(1).uuidValue().toString(),
        "projectIdentifier", row.findCell(2).stringValue()));
  }

  @Override
  public CompletableFuture<List<UUID>> findEntries(DatabaseCondition condition) {
    return contentDatabaseTable.findContentByCondition(condition).thenApply(
      rows -> rows.stream().map(row -> row.findCell(0).uuidValue()).toList());
  }

  @Override
  public CompletableFuture<Void> delete(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId)
      .thenCompose(row -> gitlabWebhookFactory.build(row.findCell(1).uuidValue(),
          row.findCell(2).stringValue()).delete(row.findCell(3).stringValue())
        .thenCompose(value -> contentDatabaseTable.deleteContent(triggerId)));
  }
}
