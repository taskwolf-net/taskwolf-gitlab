package net.taskwolf.gitlab.structure;

public enum GitlabType {
  OFFICIAL,
  SELF_HOSTED;

  public boolean isOfficial() {
    return this == OFFICIAL;
  }

  public boolean isSelfHosted() {
    return this == SELF_HOSTED;
  }
}
