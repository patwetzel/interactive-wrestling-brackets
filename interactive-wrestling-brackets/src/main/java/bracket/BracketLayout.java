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
    int baseStep = MATCH_CARD_HEIGHT + ROUND_OF_32_GAP;
    double offset = ((1 << roundDepth) - 1) * (baseStep / 2.0);
    return (int) Math.floor(offset);
  }

  public static int calculateBetweenGap(int roundDepth) {
    if (roundDepth < 0) {
      return 8;
    }
    int baseStep = MATCH_CARD_HEIGHT + ROUND_OF_32_GAP;
    int roundStep = baseStep * (1 << roundDepth);
    return roundStep - MATCH_CARD_HEIGHT;
  }
}
