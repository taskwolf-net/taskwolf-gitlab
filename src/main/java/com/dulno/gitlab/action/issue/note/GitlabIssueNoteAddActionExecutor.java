package com.dulno.gitlab.action.issue.note;

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
public final class GitlabIssueNoteAddActionExecutor implements ActionExecutor {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private final String projectId;
  private String issueId;
  private String issueNote;

  @Override
  public CompletableFuture<ActionResult> execute(Map<String, Object> information) {
    var dissolve = PlaceholderDissolve.create(information);
    issueId = dissolve.dissolve(issueId);
    issueNote = dissolve.dissolve(issueNote);
    return gitlabDatabaseTable.gitlabExists(gitlabId).thenCompose(this::execute);
  }

  private CompletableFuture<ActionResult> execute(boolean gitlabExists) {
    if (!gitlabExists) {
      return ActionResult.futureFailure("gitlab.action.issue.note.add.failure.gitlab.not.found");
    }
    var body = Map.<String, Object>of("body", issueNote);
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/issues/" + issueId + "/notes",
        "POST", body)
      .thenApply(this::execute);
  }

  private ActionResult execute(HttpResponse<String> response) {
    if (response.statusCode() != 201) {
      return ActionResult.failure("gitlab.action.issue.note.add.failure.gitlab.response");
    }
    return ActionResult.success(buildInformation(
      new JSONObject(response.body()).getInt("id")));
  }

  private Map<String, Object> buildInformation(int issueNoteIdentifier) {
    var information = Maps.<String, Object>newHashMap();
    information.put("issueNoteIdentifier", String.valueOf(issueNoteIdentifier));
    information.put("issueNote", issueNote);
    return information;
  }
}
