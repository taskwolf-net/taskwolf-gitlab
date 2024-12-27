package com.dulno.gitlab.trigger.note;

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
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class GitlabNoteAddTrigger implements Trigger {
  public static GitlabNoteAddTrigger create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabWebhookFactory gitlabWebhookFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    return new GitlabNoteAddTrigger(gitlabComponentSelect,
      projectComponentSelect, gitlabWebhookFactory,
      TriggerGitlabDatabaseTable.create(databaseConnection, databaseKeyspace,
        "trigger_gitlab_note_add"));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabWebhookFactory gitlabWebhookFactory;
  private final TriggerGitlabDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-note-add-trigger";
  }

  @Override
  public TriggerInformation information() {
    return TriggerInformation.builder()
      .withName("gitlab.trigger.note.add.name")
      .withDescription("gitlab.trigger.note.add.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.note.add.input.gitlab.name",
        "gitlabIdentifier", "gitlab.trigger.note.add.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.trigger.note.add.input.project.name",
        "projectIdentifier", "gitlab.trigger.note.add.input.project.description", projectComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.note.add.output.identifier", "noteIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.note.add.output.content", "noteContent"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.note.add.output.noteable.type", "noteableType"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.note.add.output.noteable.identifier", "noteableIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.note.add.output.author.name", "noteAuthorName"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.trigger.note.add.output.author.email", "noteAuthorEmail"))
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
      .create(Lists.newArrayList("note_events", "issues_events",
        "merge_requests_events"), webhookSecret)
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
