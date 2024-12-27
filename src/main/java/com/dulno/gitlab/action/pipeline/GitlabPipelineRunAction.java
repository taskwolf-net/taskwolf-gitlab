package com.dulno.gitlab.action.pipeline;

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
public final class GitlabPipelineRunAction implements Action<GitlabPipelineRunActionExecutor> {
  public static GitlabPipelineRunAction create(
    InputComponentSelect gitlabComponentSelect,
    InputComponentSelect projectComponentSelect,
    GitlabDatabaseTable gitlabDatabaseTable,
    GitlabRequestFactory gitlabRequestFactory,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var contentColumns = Lists.<DatabaseColumn>newArrayList();
    contentColumns.add(DatabaseColumn.create("gitlabId", DatabaseDataType.UUID));
    contentColumns.add(DatabaseColumn.create("projectId", DatabaseDataType.TEXT));
    contentColumns.add(DatabaseColumn.create("pipelineReference", DatabaseDataType.TEXT));
    return new GitlabPipelineRunAction(gitlabComponentSelect,
      projectComponentSelect, gitlabDatabaseTable, gitlabRequestFactory,
      ActionContentDatabaseTable.create(databaseConnection, databaseKeyspace,
        "action_gitlab_pipeline_run", contentColumns));
  }

  private final InputComponentSelect gitlabComponentSelect;
  private final InputComponentSelect projectComponentSelect;
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final ActionContentDatabaseTable contentDatabaseTable;

  @Override
  public String type() {
    return "gitlab-pipeline-run-action";
  }

  @Override
  public ActionInformation information() {
    return ActionInformation.builder()
      .withName("gitlab.action.pipeline.run.name")
      .withDescription("gitlab.action.pipeline.run.description")
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.pipeline.run.input.gitlab.name",
        "gitlabIdentifier", "gitlab.action.pipeline.run.input.gitlab.description", gitlabComponentSelect))
      .withInputVariable(InputComponentVariable.createSelect("gitlab.action.pipeline.run.input.project.name",
        "projectIdentifier", "gitlab.action.pipeline.run.input.project.description", projectComponentSelect))
      .withInputVariable(InputComponentVariable.createRequired("gitlab.action.pipeline.run.input.reference.name",
        "pipelineReference", "gitlab.action.pipeline.run.input.reference.description", InputComponentDataType.TEXT))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.pipeline.run.output.identifier", "pipelineIdentifier"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.pipeline.run.output.reference", "pipelineReference"))
      .withOutputVariable(OutputComponentVariable.create("gitlab.action.pipeline.run.output.web", "pipelineWebUrl"))
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
      content.get("projectIdentifier"), content.get("pipelineReference")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(1).uuidValue().toString(),
        "projectIdentifier", row.findCell(2).stringValue(),
        "pipelineReference", row.findCell(3).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabPipelineRunActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabPipelineRunActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory,
        content.findCell(1).uuidValue(), content.findCell(2).stringValue(),
        content.findCell(3).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}