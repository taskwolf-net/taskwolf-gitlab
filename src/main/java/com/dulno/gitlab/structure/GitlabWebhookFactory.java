package com.dulno.gitlab.structure;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class GitlabWebhookFactory {
  private final GitlabRequestFactory gitlabRequestFactory;

  public GitlabWebhook build(UUID gitlabId, String projectId) {
    return GitlabWebhook.build(gitlabRequestFactory, gitlabId, projectId);
  }
}
