package net.taskwolf.gitlab.select;

import net.taskwolf.core.user.User;
import net.taskwolf.workflow.component.input.InputComponentSelect;
import net.taskwolf.workflow.component.input.InputComponentSelectEntry;
import net.taskwolf.gitlab.structure.GitlabDatabaseTable;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public class GitlabComponentSelect implements InputComponentSelect {
  private final GitlabDatabaseTable gitlabDatabaseTable;

  @Override
  public CompletableFuture<List<InputComponentSelectEntry>> compile(
    User user, UUID target, Map<String, String> previousInputs
  ) {
    return gitlabDatabaseTable.findGitlabsOfOwner(target)
      .thenApply(gitlabs -> gitlabs.stream()
        .map(gitlab -> InputComponentSelectEntry.create(gitlab.id().toString(),
          gitlab.hostname() + " | " + gitlab.accountUsername()))
        .toList());
  }
}