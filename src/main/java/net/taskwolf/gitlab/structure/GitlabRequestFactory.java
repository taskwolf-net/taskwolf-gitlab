package net.taskwolf.gitlab.structure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class GitlabRequestFactory {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public GitlabRequest create(UUID gitlabId) {
    return GitlabRequest.create(gitlabDatabaseTable, httpClient, gitlabId);
  }
}
