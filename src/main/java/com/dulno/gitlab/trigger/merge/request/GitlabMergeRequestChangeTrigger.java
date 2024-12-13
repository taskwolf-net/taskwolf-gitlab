package com.dulno.gitlab.trigger.merge.request;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.database.condition.DatabaseCondition;
import com.dulno.core.trigger.Trigger;
import com.dulno.core.trigger.TriggerInformation;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.core.workflow.component.input.InputComponentVariable;
import com.dulno.core.workflow.component.output.OutputComponentVariable;
import com.dulno.gitlab.structure.GitlabWebhookFactory;
import com.dulno.gitlab.trigger.TriggerGitlabDatabaseTable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class GitlabMergeRequestChangeTrigger implements Trigger {
  public static GitlabMergeRequestChangeTrigger create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabWebhookFactory gitlabWebhookFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    return new GitlabMergeRequestChangeTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory,
      TriggerGitlabDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_merge_request_change"));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final TriggerGitlabDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-merge-request-change-trigger";
  }

  @Override
  public TriggerInformation information() {
    return TriggerInformation.builder()
      .withName("gitlab.trigger.merge.request.change.name")
      .withDescription("gitlab.trigger.merge.request.change.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.merge.request.change.input.gitlab.name",
        "gitlabIdentifier", "gitlab.trigger.merge.request.change.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.merge.request.change.input.project.name",
        "projectIdentifier", "gitlab.trigger.merge.request.change.input.project.description", projectComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.identifier", "mergeRequestIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.title", "mergeRequestTitle"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.description", "mergeRequestDescription"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.web", "mergeRequestWebUrl"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.status", "mergeRequestStatus"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.source.branch", "mergeRequestSourceBranch"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.target.branch", "mergeRequestTargetBranch"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.creator.name", "mergeRequestCreatorName"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.merge.request.change.output.creator.email", "mergeRequestCreatorEmail"))
      .build();
  }

  @Override
  public void initialize() {
    contentDatabaseTable.initialize();
  }

  @Override
  public CompletableFuture<Void> insert(UUID triggerId, Map<String, Object> content) {
    var gitlabId = UUID.fromString((String) content.get("gitlabIdentifier"));
    var projectId = (String) content.get("projectIdentifier");
    var webhookSecret = UUID.randomUUID().toString();
    return gitlabWebhookFactory.build(gitlabId, projectId)
      .create("merge_requests_events", webhookSecret)
      .thenCompose(webhookId -> contentDatabaseTable.insertContent(triggerId,
        gitlabId, projectId, webhookId, webhookSecret));
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
          row.findCell(1).stringValue()).delete(row.findCell(4).stringValue())
        .thenCompose(value -> contentDatabaseTable.deleteContent(triggerId)));
  }
}
