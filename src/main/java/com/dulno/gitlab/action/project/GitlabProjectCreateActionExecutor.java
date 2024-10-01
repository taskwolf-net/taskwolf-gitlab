package com.dulno.gitlab.action.project;

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
public final class GitlabProjectCreateActionExecutor implements ActionExecutor {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private String projectName;
  private final String visibility;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    projectName = dissolve.dissolve(projectName);
    return gitlabDatabaseTable.gitlabExists(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(boolean gitlabExists) {
    if (!gitlabExists) {
      return ActionResult.futureFailure("gitlab.action.project.create.failure.gitlab.not.found");
    }
    var body = Map.<String, Object>of("name", projectName,
      "visibility", visibility, "initialize_with_readme", false);
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects", "POST", body).thenApply(this::execute);
  }

  private ActionResult execute(HttpResponse<String> response) {
    if (response.statusCode() != 201) {
      return ActionResult.failure("gitlab.action.project.create.failure.gitlab.response");
    }
    return ActionResult.success(buildInformation(
      new JSONObject(response.body()).getInt("id")));
  }

  private Map<String, Object> buildInformation(int projectIdentifier) {
    var information = Maps.<String, Object>newHashMap();
    information.put("projectIdentifier", String.valueOf(projectIdentifier));
    information.put("projectName", projectName);
    return information;
  }
}
