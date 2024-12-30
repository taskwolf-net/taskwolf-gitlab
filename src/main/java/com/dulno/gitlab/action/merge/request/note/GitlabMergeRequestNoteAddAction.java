package com.dulno.gitlab.action.merge.request.note;

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
public final class GitlabMergeRequestNoteAddAction implements Action<GitlabMergeRequestNoteAddActionExecutor> {
  public static GitlabMergeRequestNoteAddAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("ownerId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("mergeRequestId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("mergeRequestNote", DatabaseDataType.TEXT));
    return new GitlabMergeRequestNoteAddAction(gitlabComponentSelect,
      projectComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_merge_request_note_add", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-merge-request-note-add-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.merge.request.note.add.name")
      .withDescription("gitlab.action.merge.request.note.add.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.merge.request.note.add.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.merge.request.note.add.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.merge.request.note.add.input.project.name",
        "projectIdentifier", "gitlab.action.merge.request.note.add.input.project.description", projectComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.note.add.input.merge.request.name",
        "mergeRequestIdentifier", "gitlab.action.merge.request.note.add.input.merge.request.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.note.add.input.note.name",
        "mergeRequestNote", "gitlab.action.merge.request.note.add.input.note.description", InputComponentDataType.TEXT_AREA))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.note.add.output.identifier", "mergeRequestNoteIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.note.add.output.note", "mergeRequestNote"))
      .build();
  }

  @Override
  public void initialize() {
    contentDatabaseTable.createIfNotExists();
  }

  @Override
  public CompletableFuture<Void> insert(
    UUID actionId, UUID ownerId, Map<String, Object> content
  ) {
    return contentDatabaseTable.insertContent(actionId, DatabaseRow.of(ownerId,
      UUID.fromString((String) content.get("gitlabIdentifier")),
      content.get("projectIdentifier"), content.get("mergeRequestIdentifier"),
      content.get("mergeRequestNote")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(2).uuidValue().toString(),
        "projectIdentifier", row.findCell(3).stringValue(),
        "mergeRequestIdentifier", row.findCell(4).stringValue(),
        "mergeRequestNote", row.findCell(5).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabMergeRequestNoteAddActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabMergeRequestNoteAddActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory, content.findCell(1).uuidValue(),
        content.findCell(2).uuidValue(), content.findCell(3).stringValue(),
        content.findCell(4).stringValue(), content.findCell(5).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}