package bracket;

import lombok.experimental.UtilityClass;
import match.MatchNode;

import javax.swing.JButton;
import java.awt.Color;

import static constants.Constants.BUTTON_BASE_COLOR;
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
    applyBaseButtonStyle(node.getButtonOne(), BUTTON_BASE_COLOR);
    applyBaseButtonStyle(node.getButtonTwo(), BUTTON_BASE_COLOR);
  }

  private static void applyBaseButtonStyle(JButton button, Color background) {
    button.setBackground(background);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
  }
}
