package com.dulno.gitlab.action.pipeline;

import com.dulno.core.action.ActionExecutor;
import com.dulno.core.action.ActionResult;
import com.dulno.core.workflow.placeholder.PlaceholderDissolve;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.json.JSONObject;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class GitlabPipelineRunActionExecutor implements ActionExecutor {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private final String projectId;
  private String pipelineReference;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    pipelineReference = dissolve.dissolve(pipelineReference);
    return gitlabDatabaseTable.gitlabExists(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(boolean gitlabExists) {
    if (!gitlabExists) {
      return ActionResult.futureFailure("gitlab.action.pipeline.run.failure.gitlab.not.found");
    }
    var body = Map.<String, Object>of("ref", pipelineReference);
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/pipeline", "POST", body)
      .thenApply(this::execute);
  }

  private ActionResult execute(HttpResponse<String> response) {
    if (response.statusCode() != 201) {
      return ActionResult.failure("gitlab.action.pipeline.run.failure.gitlab.response");
    }
    var responseBody = new JSONObject(response.body());
    return ActionResult.success(buildInformation(
      responseBody.getInt("id"), responseBody.getString("web_url")));
  }

  private Map<String, Object> buildInformation(
    int pipelineIdentifier, String pipelineWebUrl
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("pipelineIdentifier", String.valueOf(pipelineIdentifier));
    information.put("pipelineReference", pipelineReference);
    information.put("pipelineWebUrl", pipelineWebUrl);
    return information;
  }
}