package bracket;

import static constants.Constants.MATCH_CARD_HEIGHT;
import static constants.Constants.ROUND_OF_32_GAP;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BracketLayout {

  public static int calculateTopOffset(int roundDepth) {
    if (roundDepth <= 0) {
      return 0;
    }
    double offset = ((1 << roundDepth) - 1) * (baseStep() / 2.0);
    return (int) Math.round(offset);
  }

  public static int calculateBetweenGap(int roundDepth) {
    if (roundDepth < 0) {
      return 8;
    }
    int roundStep = baseStep() * (1 << roundDepth);
    return roundStep - MATCH_CARD_HEIGHT;
  }

  private static int baseStep() {
    return MATCH_CARD_HEIGHT + ROUND_OF_32_GAP;
  }
}
