package com.dulno.gitlab.action.merge.request;

import com.dulno.gitlab.structure.Gitlab;
import com.dulno.workflow.action.ActionExecutor;
import com.dulno.workflow.action.ActionResult;
import com.dulno.workflow.placeholder.PlaceholderDissolve;
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
public final class GitlabMergeRequestCreateActionExecutor implements ActionExecutor {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID ownerId;
  private final UUID gitlabId;
  private final String projectId;
  private String mergeRequestTitle;
  private String mergeRequestDescription;
  private String sourceBranch;
  private String targetBranch;
  private final boolean deleteSourceBranch;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    mergeRequestTitle = dissolve.dissolve(mergeRequestTitle);
    mergeRequestDescription = dissolve.dissolve(mergeRequestDescription);
    sourceBranch = dissolve.dissolve(sourceBranch);
    targetBranch = dissolve.dissolve(targetBranch);
    return gitlabDatabaseTable.gitlabExists(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(boolean gitlabExists) {
    if (!gitlabExists) {
      return ActionResult.futureFailure("gitlab.action.merge.request.create.failure.gitlab.not.found");
    }
    return gitlabDatabaseTable.findGitlab(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(Gitlab gitlab) {
    if (!gitlab.ownerId().equals(ownerId)) {
      return ActionResult.futureFailure("gitlab.action.merge.request.create.failure.gitlab.not.found");
    }
    var body = Map.<String, Object>of("title", mergeRequestTitle,
      "description", mergeRequestDescription, "source_branch", sourceBranch,
      "target_branch", targetBranch, "remove_source_branch", deleteSourceBranch);
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/merge_requests", "POST", body)
      .thenApply(this::execute);
  }

  private ActionResult execute(HttpResponse<String> response) {
    if (response.statusCode() != 201) {
      return ActionResult.failure("gitlab.action.merge.request.create.failure.gitlab.response");
    }
    return ActionResult.success(buildInformation(
      new JSONObject(response.body()).getInt("iid")));
  }

  private Map<String, Object> buildInformation(int mergeRequestIdentifier) {
    var information = Maps.<String, Object>newHashMap();
    information.put("mergeRequestIdentifier",
      String.valueOf(mergeRequestIdentifier));
    information.put("mergeRequestTitle", mergeRequestTitle);
    information.put("mergeRequestDescription", mergeRequestDescription);
    information.put("sourceBranch", sourceBranch);
    information.put("targetBranch", targetBranch);
    return information;
  }
}
