package com.dulno.gitlab.access;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.access.DulnoRestController;
import com.dulno.core.organization.team.TeamDatabaseTable;
import com.dulno.core.organization.team.TeamTargetDatabaseTable;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.core.user.UserTargetDatabaseTable;
import com.dulno.gitlab.structure.Gitlab;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class GitlabController extends DulnoRestController {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final UserTargetDatabaseTable userTargetDatabaseTable;
  private final TeamTargetDatabaseTable teamTargetDatabaseTable;
  private final TeamDatabaseTable teamDatabaseTable;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  private GitlabController(
    Key productKey, UserDatabaseTable userDatabaseTable,
    GitlabDatabaseTable gitlabDatabaseTable,
    UserTargetDatabaseTable userTargetDatabaseTable,
    TeamTargetDatabaseTable teamTargetDatabaseTable,
    TeamDatabaseTable teamDatabaseTable
  ) {
    super(productKey, userDatabaseTable);
    this.gitlabDatabaseTable = gitlabDatabaseTable;
    this.userTargetDatabaseTable = userTargetDatabaseTable;
    this.teamTargetDatabaseTable = teamTargetDatabaseTable;
    this.teamDatabaseTable = teamDatabaseTable;
  }

  @RequestMapping(path = "/gitlab/add/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> addGitlab(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var hostname = body.getString("hostname").replace("/", "").replace(":", "")
      .replace("https", "").replace("http", "");
    return findUser(request)
      .thenCompose(user -> userTargetDatabaseTable.findTargetSecured(user.id())
        .thenCompose(target -> findGitlabOwner(user, target)
          .thenCompose(owner -> gitlabDatabaseTable.gitlabExists(owner, hostname)
            .thenCompose(gitlabExists -> addGitlab(owner, hostname,
              body.getString("applicationId"), body.getString("secret"),
              gitlabExists)))));
  }

  private CompletableFuture<UUID> findGitlabOwner(User user, UUID target) {
    return user.id().equals(target) ?
      CompletableFuture.completedFuture(target) :
      teamTargetDatabaseTable.findTargetSecured(user.id())
        .thenApply(team -> team.orElse(target));
  }

  private static final String GITLAB_REDIRECT = "https://%s/oauth/authorize?" +
    "client_id=%s&redirect_uri=https://api.dulno.com/gitlab/authorize/&" +
    "response_type=code&state=%s";

  private CompletableFuture<Map<String, Object>> addGitlab(
    UUID ownerId, String hostname, String applicationId, String secret,
    boolean gitlabExists
  ) {
    if (gitlabExists) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return gitlabDatabaseTable.generateAvailableGitlabId()
      .thenApply(id -> addGitlab(id, ownerId, hostname, applicationId, secret));
  }

  private Map<String, Object> addGitlab(
    UUID id, UUID ownerId, String hostname, String applicationId, String secret
  ) {
    var type = hostname.contains("gitlab.com") ? GitlabType.OFFICIAL :
      GitlabType.SELF_HOSTED;
    var redirect = String.format(GITLAB_REDIRECT, hostname, applicationId,
      id.toString());
    gitlabDatabaseTable.insertGitlab(id, ownerId, type, hostname,
      applicationId, secret, "", -1, "");
    return Map.of("success", true, "redirect", redirect);
  }

  @RequestMapping(path = "/gitlab/authorize/", method = RequestMethod.GET)
  public void authorizeGitlab(
    HttpServletRequest request, @RequestParam("code") String code,
    @RequestParam("state") String state, HttpServletResponse response
  ) throws Exception {
    response.sendRedirect("https://dulno.com/close/");
    var gitlabId = UUID.fromString(state);
    findUser(request).thenAccept(user -> gitlabDatabaseTable.exists(gitlabId)
      .thenAccept(exists -> authorizeGitlab(user, gitlabId, code, exists)));
  }

  private void authorizeGitlab(
    User user, UUID gitlabId, String code, boolean gitlabExists
  ) {
    if (!gitlabExists) {
      return;
    }
    gitlabDatabaseTable.findGitlab(gitlabId)
      .thenAccept(gitlab -> checkGitlabAuthorization(user, gitlab)
        .thenAccept(authorized -> authorizeGitlab(gitlab, code, authorized)));
  }

  private static final String GITLAB_TOKEN_URL = "https://%s/oauth/token";

  private void authorizeGitlab(
    Gitlab gitlab, String code, boolean hasAuthorization
  ) {
    if (!hasAuthorization) {
      return;
    }
    var payload = new JSONObject(Map.of("client_id", gitlab.applicationId(),
      "client_secret", gitlab.secret(), "code", code,
      "grant_type", "authorization_code",
      "redirect_uri", "https://api.dulno.com/gitlab/authorize/")).toString();
    var requestBuilder = HttpRequest.newBuilder().uri(URI.create(
      String.format(GITLAB_TOKEN_URL, gitlab.hostname())))
      .method("POST", HttpRequest.BodyPublishers.ofString(payload));
    var httpRequest = requestBuilder.build();
    httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
      .thenAccept(response -> updateGitlabAccess(gitlab, response));
  }

  private void updateGitlabAccess(Gitlab gitlab, HttpResponse<String> httpResponse) {
    var result = new JSONObject(httpResponse.body());
    gitlabDatabaseTable.updateGitlabAccess(gitlab, result.getString("access_token"),
      System.currentTimeMillis() + result.getLong("expires_in") * 1000,
      result.getString("refresh_token"));
  }

  private CompletableFuture<Boolean> checkGitlabAuthorization(
    User user, Gitlab gitlab
  ) {
    var owner = gitlab.ownerId();
    if (owner.equals(user.id()) || user.organizations().contains(owner)) {
      return CompletableFuture.completedFuture(true);
    }
    return teamDatabaseTable.teamExists(owner)
      .thenCompose(exists -> exists ?
        teamDatabaseTable.findTeam(owner)
          .thenApply(team -> team.members().contains(user.id())) :
        CompletableFuture.completedFuture(false));
  }
}
