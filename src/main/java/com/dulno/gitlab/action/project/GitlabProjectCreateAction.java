package com.dulno.gitlab.action.project;

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
public final class GitlabProjectCreateAction implements Action<GitlabProjectCreateActionExecutor> {
  public static GitlabProjectCreateAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect visibilityComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("ownerId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectName", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("projectVisibility", DatabaseDataType.TEXT));
    return new GitlabProjectCreateAction(gitlabComponentSelect,
      visibilityComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_project_create", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect visibilityComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-project-create-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.project.create.name")
      .withDescription("gitlab.action.project.create.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.project.create.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.project.create.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.project.create.input.project.name",
        "projectName", "gitlab.action.project.create.input.project.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.project.create.input.visibility.name",
        "projectVisibility", "gitlab.action.project.create.input.visibility.description", visibilityComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.project.create.output.identifier", "projectIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.project.create.output.name", "projectName"))
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
      content.get("projectName"), content.get("projectVisibility")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(2).uuidValue().toString(),
        "projectName", row.findCell(3).stringValue(),
        "projectVisibility", row.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabProjectCreateActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabProjectCreateActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory, content.findCell(1).uuidValue(),
        content.findCell(2).uuidValue(), content.findCell(3).stringValue(),
        content.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}