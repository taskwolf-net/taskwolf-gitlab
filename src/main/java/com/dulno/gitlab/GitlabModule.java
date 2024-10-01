package com.dulno.gitlab;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.locale.Translation;
import com.dulno.gitlab.action.issue.GitlabIssueCreateAction;
import com.dulno.gitlab.action.merge.request.GitlabMergeRequestCreateAction;
import com.dulno.gitlab.action.project.GitlabProjectCreateAction;
import com.dulno.gitlab.select.GitlabComponentSelect;
import com.dulno.gitlab.select.ProjectComponentSelect;
import com.dulno.gitlab.select.VisibilityComponentSelect;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.dulno.gitlab.structure.GitlabRequestFactory;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.dulno.core.account.AccountLink;
import com.dulno.core.action.ActionRepository;
import com.dulno.core.log.Log;
import com.dulno.core.module.Module;
import com.dulno.core.module.ModuleDescription;
import com.dulno.core.module.ModuleInformation;
import com.dulno.core.module.ModuleLoadPriority;
import com.dulno.core.trigger.TriggerRepository;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import org.springframework.boot.SpringApplication;

@ModuleDescription(name = "gitlab", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class GitlabModule extends Module {
  private Log log;
  private SpringApplication springApplication;
  private GitlabContextInitializer contextInitializer;
  private AccountLink accountLink;
  private InputComponentSelect gitlabComponentSelect;
  private InputComponentSelect projectComponentSelect;
  private InputComponentSelect visibilityComponentSelect;

  public GitlabModule(Injector injector) {
    super(injector.createChildInjector(GitlabInjectionModule.create()));
  }

  @Override
  public void enable() throws Exception {
    log = injector().getInstance(Log.class).subLog("Gitlab");
    springApplication = injector().getInstance(SpringApplication.class);
    var gitlabDatabaseTable = injector().getInstance(GitlabDatabaseTable.class);
    var gitlabRequestFactory = injector().getInstance(GitlabRequestFactory.class);
    contextInitializer = GitlabContextInitializer.create(gitlabDatabaseTable,
      gitlabRequestFactory);
    springApplication.addInitializers(contextInitializer);
    accountLink = GitlabAccountLink.create(gitlabDatabaseTable);
    gitlabComponentSelect = GitlabComponentSelect.create(gitlabDatabaseTable);
    projectComponentSelect = ProjectComponentSelect.create(gitlabRequestFactory);
    visibilityComponentSelect = VisibilityComponentSelect.create(
      injector().getInstance(Translation.class));
  }

  @Override
  public void disable() {
    var initializers = Lists.newArrayList(springApplication.getInitializers());
    initializers.remove(contextInitializer);
    springApplication.setInitializers(initializers);
  }

  @Override
  public AccountLink accountLink() {
    return accountLink;
  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("GitLab", "", "gitlab",
      ModuleInformation.Type.PUBLIC);
  }

  @Override
  public TriggerRepository triggerRepository() {
    var databaseConnection = injector().getInstance(DatabaseConnection.class);
    var databaseKeyspace = injector().getInstance(DatabaseKeyspace.class);
    var repository = TriggerRepository.create();
    return repository;
  }


  @Override
  public ActionRepository actionRepository() {
    var databaseConnection = injector().getInstance(DatabaseConnection.class);
    var databaseKeyspace = injector().getInstance(DatabaseKeyspace.class);
    var gitlabDatabaseTable = injector().getInstance(GitlabDatabaseTable.class);
    var gitlabRequestFactory = injector().getInstance(GitlabRequestFactory.class);
    var repository = ActionRepository.create();
    repository.registerAction(GitlabProjectCreateAction.create(
      gitlabComponentSelect, visibilityComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabIssueCreateAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabMergeRequestCreateAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    return repository;
  }
}