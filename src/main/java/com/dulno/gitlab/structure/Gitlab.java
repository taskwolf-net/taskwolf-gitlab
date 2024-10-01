package com.dulno.gitlab.structure;

import com.dulno.core.database.DatabaseRow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(staticName = "create")
public final class Gitlab {
  public static Gitlab of(DatabaseRow row) {
    return create(row.findCell(0).uuidValue(), row.findCell(1).uuidValue(),
      GitlabType.valueOf(row.findCell(2).stringValue()),
      row.findCell(3).stringValue(), row.findCell(4).stringValue(),
      row.findCell(5).stringValue(), row.findCell(6).stringValue(),
      row.findCell(7).stringValue(), row.findCell(8).longValue(),
      row.findCell(9).stringValue());
  }

  private final UUID id;
  private final UUID ownerId;
  private final GitlabType type;
  private final String hostname;
  private final String applicationId;
  private final String secret;
  private String accountUsername;
  private String accessToken;
  private long expiration;
  private String refreshToken;

  public void updateAccountUsername(String accountUsername) {
    this.accountUsername = accountUsername;
  }

  public void updateAccessToken(String newAccessToken) {
    this.accessToken = newAccessToken;
  }

  public void updateExpiration(long newExpiration) {
    this.expiration = newExpiration;
  }

  public void updateRefreshToken(String newRefreshToken) {
    this.refreshToken = newRefreshToken;
  }
}
