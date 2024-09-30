package com.dulno.gitlab;

import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(staticName = "create")
public final class GitlabContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final GitlabDatabaseTable gitlabDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("gitlabDatabaseTable", gitlabDatabaseTable);
  }
}
