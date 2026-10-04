package net.taskwolf.gitlab.action.issue.note;

import net.taskwolf.workflow.action.Action;
import net.taskwolf.workflow.action.ActionContentDatabaseTable;
import net.taskwolf.workflow.action.ActionInformation;
import net.taskwolf.core.database.*;
import net.taskwolf.workflow.component.input.InputComponentDataType;
import net.taskwolf.workflow.component.input.InputComponentSelect;
import net.taskwolf.workflow.component.input.InputComponentVariable;
import net.taskwolf.workflow.component.output.OutputComponentVariable;
import net.taskwolf.gitlab.structure.GitlabDatabaseTable;
import net.taskwolf.gitlab.structure.GitlabRequestFactory;
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
    contentColumns.add(DatabaseColumn.create("ownerId", DatabaseDataType.UUID));
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
  public CompletableFuture<Void> insert(
    UUID actionId, UUID ownerId, Map<String, Object> content
  ) {
    return contentDatabaseTable.insertContent(actionId, DatabaseRow.of(ownerId,
      UUID.fromString((String) content.get("gitlabIdentifier")),
      content.get("projectIdentifier"), content.get("issueIdentifier"),
      content.get("issueNote")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(2).uuidValue().toString(),
        "projectIdentifier", row.findCell(3).stringValue(),
        "issueIdentifier", row.findCell(4).stringValue(),
        "issueNote", row.findCell(5).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabIssueNoteAddActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabIssueNoteAddActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory, content.findCell(1).uuidValue(),
        content.findCell(2).uuidValue(), content.findCell(3).stringValue(),
        content.findCell(4).stringValue(), content.findCell(5).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}