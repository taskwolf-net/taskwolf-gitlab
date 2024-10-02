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
import com.dulno.gitlab.structure.GitlabRequestFactory;
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
public class GitlabConnectionController extends DulnoRestController {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final UserTargetDatabaseTable userTargetDatabaseTable;
  private final TeamTargetDatabaseTable teamTargetDatabaseTable;
  private final TeamDatabaseTable teamDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  private GitlabConnectionController(
    Key productKey, UserDatabaseTable userDatabaseTable,
    GitlabDatabaseTable gitlabDatabaseTable,
    UserTargetDatabaseTable userTargetDatabaseTable,
    TeamTargetDatabaseTable teamTargetDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GitlabRequestFactory gitlabRequestFactory
  ) {
    super(productKey, userDatabaseTable);
    this.gitlabDatabaseTable = gitlabDatabaseTable;
    this.userTargetDatabaseTable = userTargetDatabaseTable;
    this.teamTargetDatabaseTable = teamTargetDatabaseTable;
    this.teamDatabaseTable = teamDatabaseTable;
    this.gitlabRequestFactory = gitlabRequestFactory;
  }

  @RequestMapping(path = "/gitlab/add/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> addGitlab(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var hostname = body.getString("hostname").replace("/", "").replace(":", "")
      .replace("https", "").replace("http", "");
    var apiKey = request.getHeader("Authorization").replace("Bearer ", "");
    return findUser(request)
      .thenCompose(user -> userTargetDatabaseTable.findTargetSecured(user.id())
        .thenCompose(target -> findGitlabOwner(user, target)
          .thenCompose(owner -> gitlabDatabaseTable.generateAvailableGitlabId()
            .thenApply(id -> addGitlab(id, owner, hostname,
              body.getString("applicationId"), body.getString("secret"), apiKey)))));
  }

  private CompletableFuture<UUID> findGitlabOwner(User user, UUID target) {
    return user.id().equals(target) ?
      CompletableFuture.completedFuture(target) :
      teamTargetDatabaseTable.findTargetSecured(user.id())
        .thenApply(team -> team.orElse(target));
  }

  private static final String GITLAB_REDIRECT = "https://%s/oauth/authorize?" +
    "client_id=%s&redirect_uri=https://api.dulno.com/v1/gitlab/authorize/&" +
    "response_type=code&state=%s";

  private Map<String, Object> addGitlab(
    UUID id, UUID ownerId, String hostname, String applicationId, String secret,
    String apiKey
  ) {
    var type = hostname.contains("gitlab.com") ? GitlabType.OFFICIAL :
      GitlabType.SELF_HOSTED;
    var redirect = String.format(GITLAB_REDIRECT, hostname, applicationId,
      apiKey + "DULNO-STATE-SPLIT" + id.toString());
    gitlabDatabaseTable.insertGitlab(id, ownerId, type, hostname,
      applicationId, secret, "", "", -1, "");
    return Map.of("success", true, "redirect", redirect);
  }

  @RequestMapping(path = "/gitlab/authorize/", method = RequestMethod.GET)
  public void authorizeGitlab(
    @RequestParam("code") String code, @RequestParam("state") String state,
    HttpServletResponse response
  ) throws Exception {
    response.sendRedirect("https://dulno.com/close/");
    var split = state.split("DULNO-STATE-SPLIT");
    var apiKey = split[0];
    if (!isValidApiKey(apiKey)) {
      return;
    }
    var gitlabId = UUID.fromString(split[1]);
    userDatabaseTable().findUser(findUserId(apiKey))
      .thenAccept(user -> gitlabDatabaseTable.exists(gitlabId)
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
  private static final String GITLAB_TOKEN_BODY = "client_id=%s&" +
    "client_secret=%s&code=%s&grant_type=authorization_code&" +
    "redirect_uri=https://api.dulno.com/v1/gitlab/authorize/";

  private void authorizeGitlab(
    Gitlab gitlab, String code, boolean hasAuthorization
  ) {
    if (!hasAuthorization) {
      return;
    }
    var payload = String.format(GITLAB_TOKEN_BODY, gitlab.applicationId(),
      gitlab.secret(), code);
    var requestBuilder = HttpRequest.newBuilder().uri(URI.create(
      String.format(GITLAB_TOKEN_URL, gitlab.hostname())))
      .method("POST", HttpRequest.BodyPublishers.ofString(payload));
    var httpRequest = requestBuilder.build();
    httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
      .thenAccept(response -> updateGitlabAccess(gitlab, response));
  }

  private void updateGitlabAccess(
    Gitlab gitlab, HttpResponse<String> tokenResponse
  ) {
    if (tokenResponse.statusCode() != 200) {
      return;
    }
    var result = new JSONObject(tokenResponse.body());
    gitlabDatabaseTable.updateGitlabAccess(gitlab, result.getString("access_token"),
      System.currentTimeMillis() + result.getLong("expires_in") * 1000,
      result.getString("refresh_token"))
      .thenAccept(value -> gitlabRequestFactory.create(gitlab.id())
        .send("/api/v4/user", "GET", "").thenAccept(userResponse ->
          updateGitlabAccountUsername(gitlab, userResponse)));
  }

  private void updateGitlabAccountUsername(
    Gitlab gitlab, HttpResponse<String> userResponse
  ) {
    if (userResponse.statusCode() != 200) {
      return;
    }
    var result = new JSONObject(userResponse.body());
    gitlabDatabaseTable.updateGitlabAccountUsername(gitlab,
      result.getString("username"));
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
