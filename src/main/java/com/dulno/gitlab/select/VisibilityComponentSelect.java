package com.dulno.gitlab.select;

import com.dulno.core.locale.Translation;
import com.dulno.core.user.User;
import com.dulno.workflow.component.input.InputComponentSelect;
import com.dulno.workflow.component.input.InputComponentSelectEntry;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public class VisibilityComponentSelect implements InputComponentSelect {
  private final Translation translation;

  @Override
  public CompletableFuture<List<InputComponentSelectEntry>> compile(
    User user, UUID target, Map<String, String> previousInputs
  ) {
    return CompletableFuture.completedFuture(Lists.newArrayList(
      InputComponentSelectEntry.create("private",
        translation.translate(user, "gitlab.visibility.private")),
      InputComponentSelectEntry.create("internal",
        translation.translate(user, "gitlab.visibility.internal")),
      InputComponentSelectEntry.create("public",
        translation.translate(user, "gitlab.visibility.public"))));
  }
}