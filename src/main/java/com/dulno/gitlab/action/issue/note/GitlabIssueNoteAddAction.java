package com.dulno.gitlab.action.issue.note;

import com.dulno.workflow.action.Action;
import com.dulno.workflow.action.ActionContentDatabaseTable;
import com.dulno.workflow.action.ActionInformation;
import com.dulno.core.database.*;
import com.dulno.workflow.component.input.InputComponentDataType;
import com.dulno.workflow.component.input.InputComponentSelect;
import com.dulno.workflow.component.input.InputComponentVariable;
import com.dulno.workflow.component.output.OutputComponentVariable;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class GitlabIssueNoteAddAction implements Action<GitlabIssueNoteAddActionExecutor> {
  public static GitlabIssueNoteAddAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("issueId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("issueNote", DatabaseDataType.TEXT));
    return new GitlabIssueNoteAddAction(gitlabComponentSelect,
      projectComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_issue_note_add", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-issue-note-add-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.issue.note.add.name")
      .withDescription("gitlab.action.issue.note.add.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.issue.note.add.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.issue.note.add.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.issue.note.add.input.project.name",
        "projectIdentifier", "gitlab.action.issue.note.add.input.project.description", projectComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.issue.note.add.input.issue.name",
        "issueIdentifier", "gitlab.action.issue.note.add.input.issue.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.issue.note.add.input.note.name",
        "issueNote", "gitlab.action.issue.note.add.input.note.description", InputComponentDataType.TEXT_AREA))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.issue.note.add.output.identifier", "issueNoteIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.issue.note.add.output.note", "issueNote"))
      .build();
  }

  @Override
  public void initialize() {
    contentDatabaseTable.createIfNotExists();
  }

  @Override
  public CompletableFuture<Void> insert(UUID actionId, Map<String, Object> content) {
    return contentDatabaseTable.insertContent(actionId, DatabaseRow.of(
      UUID.fromString((String) content.get("gitlabIdentifier")),
      content.get("projectIdentifier"), content.get("issueIdentifier"),
      content.get("issueNote")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(1).uuidValue().toString(),
        "projectIdentifier", row.findCell(2).stringValue(),
        "issueIdentifier", row.findCell(3).stringValue(),
        "issueNote", row.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabIssueNoteAddActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabIssueNoteAddActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory,
        content.findCell(1).uuidValue(), content.findCell(2).stringValue(),
        content.findCell(3).stringValue(), content.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}