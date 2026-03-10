package match;

import constants.BracketSection;
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
  private final BracketSection bracketSection;
  @Setter
  private MatchNode nextMatch;
  @Setter
  private int nextSlot;
  @Setter
  private MatchNode loserNextMatch;
  @Setter
  private int loserNextSlot;
  private int winningSlot;
  private boolean completed;

  public MatchNode(Match match, JButton buttonOne, JButton buttonTwo, JPanel matchCard) {
    this(match, buttonOne, buttonTwo, matchCard, BracketSection.UNSPECIFIED);
  }

  public MatchNode(Match match, JButton buttonOne, JButton buttonTwo, JPanel matchCard, BracketSection bracketSection) {
    this.match = match;
    this.buttonOne = buttonOne;
    this.buttonTwo = buttonTwo;
    this.matchCard = matchCard;
    this.bracketSection = bracketSection;
    this.nextSlot = 1;
    this.loserNextSlot = 1;
    this.winningSlot = 0;
    this.completed = false;
  }

  public Wrestler selectedWinner() {
    return switch (winningSlot) {
      case 1 -> match.getWrestlerOne();
      case 2 -> match.getWrestlerTwo();
      default -> null;
    };
  }

  public Wrestler selectedLoser() {
    return switch (winningSlot) {
      case 1 -> match.getWrestlerTwo();
      case 2 -> match.getWrestlerOne();
      default -> null;
    };
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
