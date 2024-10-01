package com.dulno.gitlab.action.issue.delete;

import com.dulno.core.action.ActionExecutor;
import com.dulno.core.action.ActionResult;
import com.dulno.core.workflow.placeholder.PlaceholderDissolve;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor(staticName = "create")
public final class GitlabIssueDeleteActionExecutor implements ActionExecutor {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private final String projectId;
  private String issueId;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    issueId = dissolve.dissolve(issueId);
    return gitlabDatabaseTable.gitlabExists(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(boolean gitlabExists) {
    if (!gitlabExists) {
      return ActionResult.futureFailure("gitlab.action.issue.delete.failure.gitlab.not.found");
    }
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/issues/" + issueId, "DELETE", "")
      .thenApply(this::execute);
  }

  private ActionResult execute(HttpResponse<String> response) {
    if (response.statusCode() != 204) {
      return ActionResult.failure("gitlab.action.issue.delete.failure.gitlab.response");
    }
    return ActionResult.success(buildInformation());
  }

  private Map<String, Object> buildInformation() {
    var information = Maps.<String, Object>newHashMap();
    information.put("issueIdentifier", issueId);
    return information;
  }
}
