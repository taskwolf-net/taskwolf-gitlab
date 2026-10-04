package net.taskwolf.gitlab;

import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.locale.Translation;
import net.taskwolf.gitlab.action.issue.create.GitlabIssueCreateAction;
import net.taskwolf.gitlab.action.issue.delete.GitlabIssueDeleteAction;
import net.taskwolf.gitlab.action.issue.note.GitlabIssueNoteAddAction;
import net.taskwolf.gitlab.action.merge.request.GitlabMergeRequestCreateAction;
import net.taskwolf.gitlab.action.merge.request.note.GitlabMergeRequestNoteAddAction;
import net.taskwolf.gitlab.action.pipeline.GitlabPipelineRunAction;
import net.taskwolf.gitlab.action.project.GitlabProjectCreateAction;
import net.taskwolf.gitlab.select.GitlabComponentSelect;
import net.taskwolf.gitlab.select.ProjectComponentSelect;
import net.taskwolf.gitlab.select.VisibilityComponentSelect;
import net.taskwolf.gitlab.structure.GitlabDatabaseTable;
import net.taskwolf.gitlab.structure.GitlabRequestFactory;
import net.taskwolf.gitlab.structure.GitlabWebhookFactory;
import net.taskwolf.gitlab.trigger.commit.GitlabCommitTrigger;
import net.taskwolf.gitlab.trigger.issue.GitlabIssueChangeTrigger;
import net.taskwolf.gitlab.trigger.merge.request.GitlabMergeRequestChangeTrigger;
import net.taskwolf.gitlab.trigger.note.GitlabNoteAddTrigger;
import net.taskwolf.gitlab.trigger.pipeline.GitlabPipelineChangeTrigger;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import net.taskwolf.core.account.AccountLink;
import net.taskwolf.workflow.action.ActionRepository;
import net.taskwolf.core.log.Log;
import net.taskwolf.workflow.integration.Integration;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.workflow.trigger.TriggerRepository;
import net.taskwolf.workflow.component.input.InputComponentSelect;
import org.springframework.boot.SpringApplication;

@ModuleDescription(name = "gitlab", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class GitlabModule extends Integration {
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
    projectComponentSelect = ProjectComponentSelect.create(gitlabDatabaseTable,
      gitlabRequestFactory);
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
    var gitlabWebhookFactory = injector().getInstance(GitlabWebhookFactory.class);
    var gitlabDatabaseTable = injector().getInstance(GitlabDatabaseTable.class);
    var repository = TriggerRepository.create();
    repository.registerTrigger(GitlabCommitTrigger.create(
      gitlabComponentSelect, projectComponentSelect, gitlabWebhookFactory,
      gitlabDatabaseTable, databaseConnection, databaseKeyspace));
    repository.registerTrigger(GitlabIssueChangeTrigger.create(
      gitlabComponentSelect, projectComponentSelect, gitlabWebhookFactory,
      gitlabDatabaseTable, databaseConnection, databaseKeyspace));
    repository.registerTrigger(GitlabPipelineChangeTrigger.create(
      gitlabComponentSelect, projectComponentSelect, gitlabWebhookFactory,
      gitlabDatabaseTable, databaseConnection, databaseKeyspace));
    repository.registerTrigger(GitlabMergeRequestChangeTrigger.create(
      gitlabComponentSelect, projectComponentSelect, gitlabWebhookFactory,
      gitlabDatabaseTable, databaseConnection, databaseKeyspace));
    repository.registerTrigger(GitlabNoteAddTrigger.create(
      gitlabComponentSelect, projectComponentSelect, gitlabWebhookFactory,
      gitlabDatabaseTable, databaseConnection, databaseKeyspace));
    return repository;
  }


  @Override
  public ActionRepository actionRepository() {
    var databaseConnection = injector().getInstance(DatabaseConnection.class);
    var databaseKeyspace = injector().getInstance(DatabaseKeyspace.class);
    var gitlabDatabaseTable = injector().getInstance(GitlabDatabaseTable.class);
    var gitlabRequestFactory = injector().getInstance(GitlabRequestFactory.class);
    var repository = ActionRepository.create();
    repository.registerAction(GitlabPipelineRunAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabIssueCreateAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabIssueDeleteAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabIssueNoteAddAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabMergeRequestCreateAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabMergeRequestNoteAddAction.create(
      gitlabComponentSelect, projectComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    repository.registerAction(GitlabProjectCreateAction.create(
      gitlabComponentSelect, visibilityComponentSelect, gitlabDatabaseTable,
      gitlabRequestFactory, databaseConnection, databaseKeyspace));
    return repository;
  }
}