package match;

import lombok.Getter;
import lombok.Setter;
import wrestler.Wrestler;

import javax.swing.JButton;
import javax.swing.JPanel;

@Getter
public final class MatchNode {
  private final Match match;
  private final JButton buttonOne;
  private final JButton buttonTwo;
  private final JPanel matchCard;
  @Setter
  private MatchNode nextMatch;
  @Setter
  private int nextSlot;
  private int winningSlot;
  private boolean completed;

  public MatchNode(Match match, JButton buttonOne, JButton buttonTwo, JPanel matchCard) {
    this.match = match;
    this.buttonOne = buttonOne;
    this.buttonTwo = buttonTwo;
    this.matchCard = matchCard;
    this.nextSlot = 1;
    this.winningSlot = 0;
    this.completed = false;
  }

  public Wrestler selectedWinner() {
    if (winningSlot == 1) {
      return match.getWrestlerOne();
    }
    if (winningSlot == 2) {
      return match.getWrestlerTwo();
    }
    return null;
  }

  public void markWinner(int slot) {
    this.winningSlot = slot;
    this.completed = true;
  }

  public void clearDecision() {
    this.winningSlot = 0;
    this.completed = false;
  }
}
