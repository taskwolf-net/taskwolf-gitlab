package com.dulno.gitlab.trigger.pipeline;

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
public final class GitlabPipelineChangeTrigger implements Trigger {
  public static GitlabPipelineChangeTrigger create(
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
    return new GitlabPipelineChangeTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory,
      TriggerContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_pipeline_change", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final TriggerContentDatabaseTable contentDatabaseTable;

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
      .create("pipeline_events", webhookSecret)
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
