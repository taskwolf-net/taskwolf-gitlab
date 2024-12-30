package com.dulno.gitlab.trigger.commit;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.database.condition.DatabaseCondition;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.workflow.trigger.Trigger;
import com.dulno.workflow.trigger.TriggerInformation;
import com.dulno.workflow.component.input.InputComponentSelect;
import com.dulno.workflow.component.input.InputComponentVariable;
import com.dulno.workflow.component.output.OutputComponentVariable;
import com.dulno.gitlab.structure.GitlabWebhookFactory;
import com.dulno.gitlab.trigger.TriggerGitlabDatabaseTable;
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
    GitlabDatabaseTable gitlabDatabaseTable,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    return new GitlabCommitTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory, gitlabDatabaseTable,
      TriggerGitlabDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_commit"));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final TriggerGitlabDatabaseTable contentDatabaseTable;

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
    contentDatabaseTable.initialize();
  }

  @Override
  public CompletableFuture<Void> insert(
    UUID triggerId, UUID ownerId, Map<String, Object> content
  ) {
    var gitlabId = UUID.fromString((String) content.get("gitlabIdentifier"));
    var projectId = (String) content.get("projectIdentifier");
    var webhookSecret = UUID.randomUUID().toString();
    return gitlabWebhookFactory.build(gitlabId, projectId)
      .create("push_events", webhookSecret)
      .thenCompose(webhookId -> contentDatabaseTable.insertContent(triggerId,
        ownerId, gitlabId, projectId, webhookId, webhookSecret));
  }

  @Override
  public CompletableFuture<Boolean> checkExecution(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId)
      .thenCompose(row -> gitlabDatabaseTable.gitlabExists(
        row.findCell(0).uuidValue())
        .thenCompose(exists -> checkExecution(row.findCell(4).uuidValue(),
          row.findCell(0).uuidValue(), exists)));
  }

  public CompletableFuture<Boolean> checkExecution(
    UUID ownerId, UUID gitlabId, boolean gitlabExists
  ) {
    if (!gitlabExists) {
      return CompletableFuture.completedFuture(false);
    }
    return gitlabDatabaseTable.findGitlab(gitlabId)
      .thenApply(gitlab -> gitlab.ownerId().equals(ownerId));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(0).uuidValue().toString(),
        "projectIdentifier", row.findCell(1).stringValue()));
  }

  @Override
  public CompletableFuture<List<UUID>> findEntries(DatabaseCondition condition) {
    return contentDatabaseTable.findContentByCondition(condition).thenApply(
      rows -> rows.stream().map(row -> row.findCell(3).uuidValue()).toList());
  }

  @Override
  public CompletableFuture<Void> delete(UUID triggerId) {
    return contentDatabaseTable.findContent(triggerId)
      .thenCompose(row -> gitlabWebhookFactory.build(row.findCell(0).uuidValue(),
          row.findCell(1).stringValue()).delete(row.findCell(5).stringValue())
        .thenCompose(value -> contentDatabaseTable.deleteContent(triggerId)));
  }
}
