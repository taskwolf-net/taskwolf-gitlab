package com.dulno.gitlab.structure;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "build")
public final class GitlabWebhook {
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private final String projectId;

  private static final String GITLAB_WEBHOOK_CREATE_BODY =
    "url=https://api.dulno.com/v1/gitlab/event/&%s=true&token=%s";

  public CompletableFuture<String> create(String event, String webhookSecret) {
    return sendCreateRequest(event, gitlabId + "DULNO-STATE-SPLIT" +
      projectId + "DULNO-STATE-SPLIT" + webhookSecret);
  }

  private CompletableFuture<String> sendCreateRequest(String event, String state) {
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/hooks", "POST",
        String.format(GITLAB_WEBHOOK_CREATE_BODY, event, state),
        "application/x-www-form-urlencoded")
      .thenApply(response -> String.valueOf(
        new JSONObject(response.body()).getInt("id")));
  }

  public CompletableFuture<Void> delete(String webhookId) {
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/hooks/" + webhookId, "DELETE", "")
      .thenApply(value -> null);
  }
}
