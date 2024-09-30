package com.dulno.gitlab;

import com.dulno.core.account.AccountLinkEntry;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import lombok.RequiredArgsConstructor;
import com.dulno.core.account.AccountLink;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class GitlabAccountLink implements AccountLink {
  private final GitlabDatabaseTable gitlabDatabaseTable;

  @Override
  public CompletableFuture<Boolean> accountExists(UUID id) {
    return gitlabDatabaseTable.gitlabExistsByOwner(id);
  }

  @Override
  public CompletableFuture<List<AccountLinkEntry>> findAccounts(UUID id) {
    return gitlabDatabaseTable.findGitlabsOfOwner(id)
      .thenApply(gitlabs -> gitlabs.stream()
        .map(gitlab -> AccountLinkEntry.create(gitlab.id().toString(),
          gitlab.hostname()))
        .toList());
  }

  @Override
  public void removeAccount(UUID id, String identifier) {
    gitlabDatabaseTable.deleteGitlab(UUID.fromString(identifier));
  }

  @Override
  public String registrationUrl(UUID id, String apiKey) {
    return "https://dulno.com/gitlab/connect/";
  }

  @Override
  public String description() {
    return "gitlab.link.description";
  }
}