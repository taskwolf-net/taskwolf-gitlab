package net.taskwolf.gitlab;

import net.taskwolf.gitlab.structure.GitlabDatabaseTable;
import net.taskwolf.gitlab.structure.GitlabRequestFactory;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(staticName = "create")
public final class GitlabContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final GitlabRequestFactory gitlabRequestFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("gitlabDatabaseTable", gitlabDatabaseTable);
    beanFactory.registerSingleton("gitlabRequestFactory", gitlabRequestFactory);
  }
}
