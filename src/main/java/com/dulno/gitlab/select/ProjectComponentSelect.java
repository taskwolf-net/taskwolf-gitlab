package com.dulno.gitlab.select;

import com.dulno.gitlab.structure.Gitlab;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import com.dulno.core.user.User;
import com.dulno.workflow.component.input.InputComponentSelect;
import com.dulno.workflow.component.input.InputComponentSelectEntry;
import org.json.JSONArray;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public class ProjectComponentSelect implements InputComponentSelect {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;

  @Override
  public CompletableFuture<List<InputComponentSelectEntry>> compile(
    User user, UUID target, Map<String, String> previousInputs
  ) {
    try {
      var gitlabId = UUID.fromString(previousInputs.get("gitlabIdentifier"));
      return gitlabDatabaseTable.gitlabExists(gitlabId)
        .thenCompose(exists -> checkGitlabExistence(gitlabId, target, exists));
    } catch (Exception exception) {
      return CompletableFuture.completedFuture(Lists.newArrayList());
    }
  }

  private CompletableFuture<List<InputComponentSelectEntry>> checkGitlabExistence(
    UUID gitlabId, UUID target, boolean gitlabExists
  ) {
    if (!gitlabExists) {
      return CompletableFuture.completedFuture(Lists.newArrayList());
    }
    return gitlabDatabaseTable.findGitlab(gitlabId)
      .thenCompose(gitlab -> checkGitlabAccess(gitlab, target));
  }

  private CompletableFuture<List<InputComponentSelectEntry>> checkGitlabAccess(
    Gitlab gitlab, UUID target
  ) {
    if (!gitlab.ownerId().equals(target)) {
      return CompletableFuture.completedFuture(Lists.newArrayList());
    }
    return gitlabRequestFactory.create(gitlab.id())
      .send("/api/v4/projects", "GET", "")
      .thenApply(this::parseProjects);
  }

  private List<InputComponentSelectEntry> parseProjects(
    HttpResponse<String> response
  ) {
    if (response.statusCode() != 200) {
      return Lists.newArrayList();
    }
    var projects = new JSONArray(response.body());
    var result = Lists.<InputComponentSelectEntry>newArrayList();
    for (var i = 0; i < projects.length(); i++) {
      var project = projects.getJSONObject(i);
      result.add(InputComponentSelectEntry.create(
        String.valueOf(project.getInt("id")), project.getString("name")));
    }
    return result;
  }
}
