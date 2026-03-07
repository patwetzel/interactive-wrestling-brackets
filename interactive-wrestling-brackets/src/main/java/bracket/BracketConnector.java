package bracket;

import lombok.experimental.UtilityClass;
import match.MatchNode;

import java.util.List;

@UtilityClass
public final class BracketConnector {

  public static void connectRounds(List<MatchNode> currentRound, List<MatchNode> nextRound) {
    for (int i = 0; i < currentRound.size(); i++) {
      MatchNode current = currentRound.get(i);
      MatchNode next = nextRound.get(i / 2);
      int nextSlot = (i % 2) + 1;
      connectWinner(current, next, nextSlot);
    }
  }

  public static void connectWinner(MatchNode source, MatchNode target, int targetSlot) {
    source.setNextMatch(target);
    source.setNextSlot(targetSlot);
  }

  public static void connectLoser(MatchNode source, MatchNode target, int targetSlot) {
    source.setLoserNextMatch(target);
    source.setLoserNextSlot(targetSlot);
  }
}
