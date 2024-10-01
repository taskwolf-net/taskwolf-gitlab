package com.dulno.gitlab.structure;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class GitlabRequest {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final HttpClient httpClient;
  private final UUID gitlabId;

  public CompletableFuture<HttpResponse<String>> send(
    String url, String method, Map<String, Object> body
  ) {
    return send(url, method, new JSONObject(body).toString());
  }

  public CompletableFuture<HttpResponse<String>> send(
    String url, String method, String body
  ) {
    return gitlabDatabaseTable.findGitlab(gitlabId)
      .thenCompose(gitlab -> send(gitlab, url, method, body));
  }

  public CompletableFuture<HttpResponse<String>> send(
    Gitlab gitlab, String url, String method, String body
  ) {
    if (System.currentTimeMillis() > gitlab.expiration()) {
      return refreshAccess(gitlab)
        .thenCompose(value -> send(gitlab, url, method, body));
    }
    var requestBuilder = HttpRequest.newBuilder().uri(URI.create(
        "https://" + gitlab.hostname() + url))
      .method(method, HttpRequest.BodyPublishers.ofString(body));
    requestBuilder.setHeader("Authorization", "Bearer " + gitlab.accessToken());
    requestBuilder.setHeader("Content-Type", "application/json");
    var httpRequest = requestBuilder.build();
    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
  }

  private static final String GITLAB_REFRESH_URL = "https://%s/oauth/token";
  private static final String GITLAB_TOKEN_BODY = "client_id=%s&" +
    "client_secret=%s&refresh_token=%s&grant_type=refresh_token";

  private CompletableFuture<Void> refreshAccess(Gitlab gitlab) {
    var payload = String.format(GITLAB_TOKEN_BODY, gitlab.applicationId(),
      gitlab.secret(), gitlab.refreshToken());
    var requestBuilder = HttpRequest.newBuilder().uri(URI.create(
        String.format(GITLAB_REFRESH_URL, gitlab.hostname())))
      .method("POST", HttpRequest.BodyPublishers.ofString(payload));
    var httpRequest = requestBuilder.build();
    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
      .thenAccept(response -> updateGitlabAccess(gitlab, response));
  }

  private void updateGitlabAccess(Gitlab gitlab, HttpResponse<String> httpResponse) {
    var result = new JSONObject(httpResponse.body());
    gitlabDatabaseTable.updateGitlabAccess(gitlab, result.getString("access_token"),
      System.currentTimeMillis() + result.getLong("expires_in") * 1000,
      result.getString("refresh_token"));
  }
}
