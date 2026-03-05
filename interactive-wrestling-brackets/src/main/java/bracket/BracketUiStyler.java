package bracket;

import lombok.experimental.UtilityClass;
import match.MatchNode;

import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.Color;

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
    final Color defaultColor = UIManager.getColor("Button.background");
    applyBaseButtonStyle(node.getButtonOne(), defaultColor);
    applyBaseButtonStyle(node.getButtonTwo(), defaultColor);
  }

  private static void applyBaseButtonStyle(JButton button, Color background) {
    button.setBackground(background);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
  }
}
