package match;

import lombok.experimental.UtilityClass;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import static constants.Constants.*;

@UtilityClass
public class MatchCardFactory {

  public static JButton createWrestlerButton() {
    final JButton button = new JButton();
    button.setFont(button.getFont().deriveFont(Font.PLAIN, BUTTON_FONT_SIZE));
    button.setPreferredSize(WRESTLER_BUTTON_SIZE);
    button.setMinimumSize(WRESTLER_BUTTON_SIZE);
    button.setMaximumSize(WRESTLER_BUTTON_SIZE);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    button.setBorder(BorderFactory.createCompoundBorder(
      new LineBorder(SUBTLE_BORDER_COLOR, 1, true),
      BorderFactory.createEmptyBorder(2, 6, 2, 6)
    ));
    button.setBackground(BUTTON_BASE_COLOR);
    button.setForeground(BUTTON_TEXT_COLOR);
    button.setFocusPainted(false);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
    return button;
  }

  public static JPanel createMatchCard() {
    final JPanel matchCard = new JPanel();
    matchCard.setLayout(new BoxLayout(matchCard, BoxLayout.Y_AXIS));
    matchCard.setAlignmentX(Component.LEFT_ALIGNMENT);
    matchCard.setBorder(BorderFactory.createCompoundBorder(
      new LineBorder(MATCH_CARD_BORDER_COLOR, 1, true),
      BorderFactory.createEmptyBorder(ROUND_INNER_PADDING, ROUND_INNER_PADDING, ROUND_INNER_PADDING, ROUND_INNER_PADDING)
    ));
    matchCard.setBackground(SURFACE_COLOR);
    matchCard.setOpaque(true);
    final Dimension cardSize = new Dimension(MATCH_CARD_WIDTH, MATCH_CARD_HEIGHT);
    matchCard.setPreferredSize(cardSize);
    matchCard.setMinimumSize(cardSize);
    matchCard.setMaximumSize(cardSize);
    return matchCard;
  }
}
