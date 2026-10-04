package net.taskwolf.gitlab.trigger.pipeline;

import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.database.condition.DatabaseCondition;
import net.taskwolf.gitlab.structure.GitlabDatabaseTable;
import net.taskwolf.workflow.trigger.Trigger;
import net.taskwolf.workflow.trigger.TriggerInformation;
import net.taskwolf.workflow.component.input.InputComponentSelect;
import net.taskwolf.workflow.component.input.InputComponentVariable;
import net.taskwolf.workflow.component.output.OutputComponentVariable;
import net.taskwolf.gitlab.structure.GitlabWebhookFactory;
import net.taskwolf.gitlab.trigger.TriggerGitlabDatabaseTable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class GitlabPipelineChangeTrigger implements Trigger {
  public static GitlabPipelineChangeTrigger create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabWebhookFactory gitlabWebhookFactory,
    GitlabDatabaseTable gitlabDatabaseTable,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    return new GitlabPipelineChangeTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory, gitlabDatabaseTable,
      TriggerGitlabDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_pipeline_change"));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final TriggerGitlabDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-pipeline-change-trigger";
  }

  @Override
  public TriggerInformation information() {
    return TriggerInformation.builder()
      .withName("gitlab.trigger.pipeline.change.name")
      .withDescription("gitlab.trigger.pipeline.change.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.pipeline.change.input.gitlab.name",
        "gitlabIdentifier", "gitlab.trigger.pipeline.change.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.pipeline.change.input.project.name",
        "projectIdentifier", "gitlab.trigger.pipeline.change.input.project.description", projectComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.pipeline.change.output.identifier", "pipelineIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.pipeline.change.output.reference", "pipelineReference"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.pipeline.change.output.web", "pipelineWebUrl"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.pipeline.change.output.status", "pipelineStatus"))
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
      .create("pipeline_events", webhookSecret)
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
