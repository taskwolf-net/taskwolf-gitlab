package com.dulno.gitlab.action.merge.request;

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
public final class GitlabMergeRequestCreateAction implements Action<GitlabMergeRequestCreateActionExecutor> {
  public static GitlabMergeRequestCreateAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("mergeRequestTitle", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("mergeRequestDescription", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("sourceBranch", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("targetBranch", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("deleteSourceBranch", DatabaseDataType.BOOLEAN));
    return new GitlabMergeRequestCreateAction(gitlabComponentSelect,
      projectComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_merge_request_create", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-merge-request-create-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.merge.request.create.name")
      .withDescription("gitlab.action.merge.request.create.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.merge.request.create.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.merge.request.create.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.merge.request.create.input.project.name",
        "projectIdentifier", "gitlab.action.merge.request.create.input.project.description", projectComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.create.input.merge.title.name",
        "mergeRequestTitle", "gitlab.action.merge.request.create.input.merge.title.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.create.input.merge.description.name",
        "mergeRequestDescription", "gitlab.action.merge.request.create.input.merge.description.description", InputComponentDataType.TEXT_AREA))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.create.input.source.name",
        "sourceBranch", "gitlab.action.merge.request.create.input.source.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.create.input.target.name",
        "targetBranch", "gitlab.action.merge.request.create.input.target.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.merge.request.create.input.delete.name",
        "deleteSourceBranch", "gitlab.action.merge.request.create.input.delete.description", InputComponentDataType.BOOLEAN))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.create.output.identifier", "mergeRequestIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.create.output.title", "mergeRequestTitle"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.create.output.description", "mergeRequestDescription"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.create.output.source", "sourceBranch"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.merge.request.create.output.target", "targetBranch"))
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
      content.get("projectIdentifier"), content.get("mergeRequestTitle"),
      content.get("mergeRequestDescription"), content.get("sourceBranch"),
      content.get("targetBranch"),
      Boolean.parseBoolean((String) content.get("deleteSourceBranch"))));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(1).uuidValue().toString(),
        "projectIdentifier", row.findCell(2).stringValue(),
        "mergeRequestTitle", row.findCell(3).stringValue(),
        "mergeRequestDescription", row.findCell(4).stringValue(),
        "sourceBranch", row.findCell(5).stringValue(),
        "targetBranch", row.findCell(6).stringValue(),
        "deleteSourceBranch", String.valueOf(row.findCell(7).booleanValue())));
  }

  @Override
  public CompletableFuture<GitlabMergeRequestCreateActionExecutor> build(
    UUID actionId
  ) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabMergeRequestCreateActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory,
        content.findCell(1).uuidValue(), content.findCell(2).stringValue(),
        content.findCell(3).stringValue(), content.findCell(4).stringValue(),
        content.findCell(5).stringValue(), content.findCell(6).stringValue(),
        content.findCell(7).booleanValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}