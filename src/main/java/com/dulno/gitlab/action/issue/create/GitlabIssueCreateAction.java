package com.dulno.gitlab.action.issue.create;

import com.dulno.core.action.Action;
import com.dulno.core.action.ActionContentDatabaseTable;
import com.dulno.core.action.ActionInformation;
import com.dulno.core.database.*;
import com.dulno.core.workflow.component.input.InputComponentDataType;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.core.workflow.component.input.InputComponentVariable;
import com.dulno.core.workflow.component.output.OutputComponentVariable;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class GitlabIssueCreateAction implements Action<GitlabIssueCreateActionExecutor> {
  public static GitlabIssueCreateAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("issueTitle", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("issueDescription", DatabaseDataType.TEXT));
    return new GitlabIssueCreateAction(gitlabComponentSelect,
      projectComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_issue_create", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-issue-create-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.issue.create.name")
      .withDescription("gitlab.action.issue.create.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.issue.create.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.issue.create.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.issue.create.input.project.name",
        "projectIdentifier", "gitlab.action.issue.create.input.project.description", projectComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.issue.create.input.issue.title.name",
        "issueTitle", "gitlab.action.issue.create.input.issue.title.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.issue.create.input.issue.description.name",
        "issueDescription", "gitlab.action.issue.create.input.issue.description.description", InputComponentDataType.TEXT_AREA))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.issue.create.output.identifier", "issueIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.issue.create.output.title", "issueTitle"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.issue.create.output.description", "issueDescription"))
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
      content.get("projectIdentifier"), content.get("issueTitle"),
      content.get("issueDescription")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(1).uuidValue().toString(),
        "projectIdentifier", row.findCell(2).stringValue(),
        "issueTitle", row.findCell(3).stringValue(),
        "issueDescription", row.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabIssueCreateActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabIssueCreateActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory,
        content.findCell(1).uuidValue(), content.findCell(2).stringValue(),
        content.findCell(3).stringValue(), content.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}