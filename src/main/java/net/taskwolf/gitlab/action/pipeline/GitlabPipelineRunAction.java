package net.taskwolf.gitlab.action.pipeline;

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
public final class GitlabPipelineRunAction implements Action<GitlabPipelineRunActionExecutor> {
  public static GitlabPipelineRunAction create(
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
  public CompletableFuture<Void> insert(
    UUID actionId, UUID ownerId, Map<String, Object> content
  ) {
    return contentDatabaseTable.insertContent(actionId, DatabaseRow.of(ownerId,
      UUID.fromString((String) content.get("gitlabIdentifier")),
      content.get("projectIdentifier"), content.get("pipelineReference")));
  }

  @Override
  public CompletableFuture<Map<String, Object>> findContent(UUID actionId) {
    return contentDatabaseTable.findContent(actionId).thenApply(row ->
      Map.of("gitlabIdentifier", row.findCell(2).uuidValue().toString(),
        "projectIdentifier", row.findCell(3).stringValue(),
        "pipelineReference", row.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<GitlabPipelineRunActionExecutor> build(UUID actionId) {
    return contentDatabaseTable.findContent(actionId)
      .thenApply(content -> GitlabPipelineRunActionExecutor.create(
        gitlabDatabaseTable, gitlabRequestFactory, content.findCell(1).uuidValue(),
        content.findCell(2).uuidValue(), content.findCell(3).stringValue(),
        content.findCell(4).stringValue()));
  }

  @Override
  public CompletableFuture<Void> delete(UUID actionId) {
    return contentDatabaseTable.deleteContent(actionId);
  }
}