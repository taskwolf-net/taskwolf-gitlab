package com.dulno.gitlab.action.repository;

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
public final class GitlabRepositoryCreateAction implements Action<GitlabRepositoryCreateActionExecutor> {
  public static GitlabRepositoryCreateAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect visibilityComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("repositoryName", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("repositoryVisibility", DatabaseDataType.TEXT));
    return new GitlabRepositoryCreateAction(gitlabComponentSelect,
      visibilityComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_repository_create", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect visibilityComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-repository-create-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.repository.create.name")
      .withDescription("gitlab.action.repository.create.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.repository.create.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.repository.create.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.repository.create.input.repository.name",
        "repositoryName", "gitlab.action.repository.create.input.repository.description", InputComponentDataType.TEXT))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.repository.create.input.visibility.name",
        "repositoryVisibility", "gitlab.action.repository.create.input.visibility.description", visibilityComponentSelect))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.repository.create.output.identifier", "repositoryIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.repository.create.output.name", "repositoryName"))
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
      content.get("repositoryName"), content.get("repositoryVisibility")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(1).uuidValue().toString(),
        "repositoryName", row.findCell(2).stringValue(),
        "repositoryVisibility", row.findCell(3).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabRepositoryCreateActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabRepositoryCreateActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory,
        content.findCell(1).uuidValue(), content.findCell(2).stringValue(),
        content.findCell(3).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}