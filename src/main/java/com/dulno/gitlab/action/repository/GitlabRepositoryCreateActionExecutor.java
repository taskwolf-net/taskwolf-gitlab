package com.dulno.gitlab.action.repository;

import com.dulno.core.action.ActionExecutor;
import com.dulno.core.action.ActionResult;
import com.dulno.core.workflow.placeholder.PlaceholderDissolve;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class GitlabRepositoryCreateActionExecutor implements ActionExecutor {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private String repositoryName;
  private final String visibility;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    repositoryName = dissolve.dissolve(repositoryName);
    return gitlabDatabaseTable.gitlabExists(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(boolean gitlabExists) {
    if (!gitlabExists) {
      return ActionResult.futureFailure("gitlab.action.repository.create.failure.gitlab.not.found");
    }
    var body = Map.<String, Object>of("name", repositoryName,
      "visibility", visibility, "initialize_with_readme", false);
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects", "POST", body).thenApply(result ->
        ActionResult.success(buildInformation((int) result.get("id"))));
  }

  private Map<String, Object> buildInformation(int repositoryIdentifier) {
    var information = Maps.<String, Object>newHashMap();
    information.put("repositoryIdentifier", String.valueOf(repositoryIdentifier));
    information.put("repositoryName", repositoryName);
    return information;
  }
}
