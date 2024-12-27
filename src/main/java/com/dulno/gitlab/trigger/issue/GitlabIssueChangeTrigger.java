package com.dulno.gitlab.trigger.issue;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.database.condition.DatabaseCondition;
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
public final class GitlabIssueChangeTrigger implements Trigger {
  public static GitlabIssueChangeTrigger create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabWebhookFactory gitlabWebhookFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    return new GitlabIssueChangeTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory,
      TriggerGitlabDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_issue_change"));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final TriggerGitlabDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-issue-change-trigger";
  }

  @Override
  public TriggerInformation information() {
    return TriggerInformation.builder()
      .withName("gitlab.trigger.issue.change.name")
      .withDescription("gitlab.trigger.issue.change.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.issue.change.input.gitlab.name",
        "gitlabIdentifier", "gitlab.trigger.issue.change.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.issue.change.input.project.name",
        "projectIdentifier", "gitlab.trigger.issue.change.input.project.description", projectComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.issue.change.output.identifier", "issueIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.issue.change.output.title", "issueTitle"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.issue.change.output.description", "issueDescription"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.issue.change.output.status", "issueStatus"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.issue.change.output.creator.name", "issueCreatorName"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.issue.change.output.creator.email", "issueCreatorEmail"))
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
      .create("issues_events", webhookSecret)
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
