package team;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Team {

  private String name = "";

  public Team(String teamName) {
    setName(teamName);
  }

  public void setName(String name) {
    this.name = normalize(name);
  }

  @Override
  public String toString() {
    return name;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
