package com.dulno.gitlab.structure;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "build")
public final class GitlabWebhook {
  private final GitlabRequestFactory gitlabRequestFactory;
  private final UUID gitlabId;
  private final String projectId;

  public CompletableFuture<String> create(String event, String webhookSecret) {
    return create(Lists.newArrayList(event), webhookSecret);
  }

  public CompletableFuture<String> create(List<String> event, String webhookSecret) {
    return sendCreateRequest(event, gitlabId + "DULNO-STATE-SPLIT" +
      projectId + "DULNO-STATE-SPLIT" + webhookSecret);
  }

  private static final String GITLAB_WEBHOOK_CREATE_BODY =
    "url=https://api.dulno.com/v1/gitlab/event/&%s&token=%s";

  private CompletableFuture<String> sendCreateRequest(
    List<String> events, String state
  ) {
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/hooks", "POST",
        String.format(GITLAB_WEBHOOK_CREATE_BODY, createEventQuery(events), state),
        "application/x-www-form-urlencoded")
      .thenApply(response -> String.valueOf(
        new JSONObject(response.body()).getInt("id")));
  }

  private String createEventQuery(List<String> events) {
    var eventQuery = new StringBuilder();
    for (var i = 0; i < events.size(); i++) {
      if (i > 0) {
        eventQuery.append("&");
      }
      eventQuery.append(events.get(i));
      eventQuery.append("=true");
    }
    return eventQuery.toString();
  }

  public CompletableFuture<Void> delete(String webhookId) {
    return gitlabRequestFactory.create(gitlabId)
      .send("/api/v4/projects/" + projectId + "/hooks/" + webhookId, "DELETE", "")
      .thenApply(value -> null);
  }
}
