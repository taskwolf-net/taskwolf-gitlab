package com.dulno.gitlab.access;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.access.DulnoRestController;
import com.dulno.core.organization.team.TeamTargetDatabaseTable;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.core.user.UserTargetDatabaseTable;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class GitlabController extends DulnoRestController {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final UserTargetDatabaseTable userTargetDatabaseTable;
  private final TeamTargetDatabaseTable teamTargetDatabaseTable;

  private GitlabController(
    Key productKey, UserDatabaseTable userDatabaseTable,
    GitlabDatabaseTable gitlabDatabaseTable,
    UserTargetDatabaseTable userTargetDatabaseTable,
    TeamTargetDatabaseTable teamTargetDatabaseTable
  ) {
    super(productKey, userDatabaseTable);
    this.gitlabDatabaseTable = gitlabDatabaseTable;
    this.userTargetDatabaseTable = userTargetDatabaseTable;
    this.teamTargetDatabaseTable = teamTargetDatabaseTable;
  }

  @RequestMapping(path = "/gitlab/add/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> addGitlab(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var hostname = body.getString("hostname");
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

  private CompletableFuture<Map<String, Object>> addGitlab(
    UUID ownerId, String hostname, String applicationId, String secret,
    boolean gitlabExists
  ) {
    if (gitlabExists) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    var type = hostname.contains("gitlab.com") ? GitlabType.OFFICIAL :
      GitlabType.SELF_HOSTED;
    return gitlabDatabaseTable.generateAvailableGitlabId()
      .thenCompose(id -> gitlabDatabaseTable.insertGitlab(id, ownerId, type,
        hostname, applicationId, secret))
      .thenApply(value -> Map.of("success", true));
  }
}
