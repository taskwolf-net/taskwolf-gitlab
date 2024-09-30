package com.dulno.gitlab.select;

import com.dulno.gitlab.structure.GitlabDatabaseTable;
import lombok.RequiredArgsConstructor;
import com.dulno.core.user.User;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.core.workflow.component.input.InputComponentSelectEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public class RepositoryComponentSelect implements InputComponentSelect {
  private final GitlabDatabaseTable gitlabDatabaseTable;

  @Override
  public CompletableFuture<List<InputComponentSelectEntry>> compile(
    User user, UUID target, Map<String, String> previousInputs
  ) {
    return null;
  }
}
