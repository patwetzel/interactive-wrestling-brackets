package bracket;

import lombok.experimental.UtilityClass;
import match.MatchNode;

import javax.swing.JButton;

import static constants.Constants.LOSER_COLOR;
import static constants.Constants.WINNER_COLOR;

@UtilityClass
public class BracketUiStyler {

  public static void applyResultColors(MatchNode node) {
    if (node.getWinningSlot() == 1) {
      node.getButtonOne().setBackground(WINNER_COLOR);
      node.getButtonTwo().setBackground(LOSER_COLOR);
      return;
    }
    if (node.getWinningSlot() == 2) {
      node.getButtonOne().setBackground(LOSER_COLOR);
      node.getButtonTwo().setBackground(WINNER_COLOR);
    }
  }

  public static void resetButtonColors(MatchNode node) {
    applyBaseButtonStyle(node.getButtonOne());
    applyBaseButtonStyle(node.getButtonTwo());
  }

  private static void applyBaseButtonStyle(JButton button) {
    button.setBackground(constants.Constants.BUTTON_BASE_COLOR);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
  }
}
