package wrestler;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WrestlerLabelFormatter {

  public static String format(Wrestler wrestler) {
    if (wrestler == null) {
      return "TBD";
    }

    int seed = wrestler.getSeed();
    if (seed <= 0) {
      return wrestler.getName() + " (" + wrestler.getTeam() + ") " + wrestler.getWins() + "-" + wrestler.getLosses();
    }

    return "(" + seed + ") " + wrestler.getName() + " (" + wrestler.getTeam() + ") " + wrestler.getWins() + "-" + wrestler.getLosses();
  }
}
