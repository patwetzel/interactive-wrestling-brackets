package wrestler;

import lombok.Getter;
import team.Team;

@Getter
public class Wrestler {

  private String name = "";
  private Team team = new Team();
  private int seed;
  private int wins;
  private int losses;

  public Wrestler(String name, Team team, int seed, int wins, int losses) {
    setName(name);
    setTeam(team);
    setSeed(seed);
    setWins(wins);
    setLosses(losses);
  }

  public void setName(String name) {
    this.name = normalize(name);
  }

  public void setTeam(Team team) {
    this.team = team == null ? new Team() : team;
  }

  public void setSeed(int seed) {
    this.seed = Math.max(0, seed);
  }

  public void setWins(int wins) {
    this.wins = Math.max(0, wins);
  }

  public void setLosses(int losses) {
    this.losses = Math.max(0, losses);
  }

  @Override
  public String toString() {
    return String.format("%s (%s) %d-%d", name, team, wins, losses);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
