package com.dulno.gitlab;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.gitlab.structure.GitlabDatabaseTable;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public class GitlabInjectionModule extends AbstractModule {
  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  GitlabDatabaseTable provideGitlabDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var gitlabDatabaseTable = GitlabDatabaseTable.create(connection, keyspace);
    gitlabDatabaseTable.createIfNotExists();
    gitlabDatabaseTable.createIndexIfNotExists("owner");
    return gitlabDatabaseTable;
  }
}
