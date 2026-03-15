package bracket.ui;

import static constants.Constants.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class TeamScoresPanelBuilder {

  private static final int TEAM_LABEL_WIDTH = 176;
  private static final int SCORE_LABEL_WIDTH = 52;
  private static final int ROW_WIDTH = 240;

  private final String headerText;
  private final List<Color> rowColors = List.of(
    new Color(255, 243, 196),
    new Color(236, 240, 243),
    new Color(242, 214, 191)
  );
  private final Color defaultRowColor = new Color(220, 236, 255);

  public TeamScoresPanelBuilder(String headerText) {
    this.headerText = headerText;
  }

  public JPanel buildTeamScoresPanel(Map<String, Double> teamScores) {
    final JPanel container = new JPanel();
    container.setOpaque(false);
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    container.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_PANEL_GAP, ROUND_OUTER_PADDING, ROUND_PANEL_GAP));

    final JLabel header = new JLabel(headerText, SwingConstants.CENTER);
    header.setFont(header.getFont().deriveFont(Font.BOLD, ROUND_HEADER_FONT_SIZE + 2));
    header.setForeground(BUTTON_TEXT_COLOR);
    final JPanel headerRow = new JPanel(new BorderLayout());
    headerRow.setOpaque(false);
    headerRow.setAlignmentX(Component.CENTER_ALIGNMENT);
    headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROUND_HEADER_HEIGHT));
    headerRow.add(header, BorderLayout.CENTER);
    container.add(headerRow);
    container.add(Box.createVerticalStrut(12));

    final List<Map.Entry<String, Double>> sorted = new ArrayList<>(teamScores.entrySet());
    sorted.sort(Comparator
      .comparingDouble((Map.Entry<String, Double> entry) -> entry.getValue()).reversed()
      .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
    );

    if (sorted.isEmpty()) {
      container.add(createEmptyRow());
      return container;
    }

    container.add(createHeaderRow());
    container.add(Box.createVerticalStrut(6));

    for (int i = 0; i < sorted.size(); i++) {
      final Map.Entry<String, Double> entry = sorted.get(i);
      container.add(createScoreRow(i + 1, entry.getKey(), entry.getValue()));
      if (i < sorted.size() - 1) {
        container.add(Box.createVerticalStrut(4));
      }
    }

    return container;
  }

  private JPanel createHeaderRow() {
    final JPanel row = createCenteredRow();
    row.setOpaque(false);
    row.setAlignmentX(Component.CENTER_ALIGNMENT);
    row.setMaximumSize(new Dimension(ROW_WIDTH, 26));
    row.setPreferredSize(new Dimension(ROW_WIDTH, 26));

    final JLabel teamLabel = new JLabel("Team", SwingConstants.LEFT);
    teamLabel.setFont(teamLabel.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE + 1));
    teamLabel.setForeground(BUTTON_TEXT_COLOR);
    teamLabel.setPreferredSize(new Dimension(TEAM_LABEL_WIDTH, 26));
    teamLabel.setMinimumSize(new Dimension(TEAM_LABEL_WIDTH, 26));

    final JLabel scoreLabel = new JLabel("Points", SwingConstants.RIGHT);
    scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE + 1));
    scoreLabel.setForeground(BUTTON_TEXT_COLOR);
    scoreLabel.setPreferredSize(new Dimension(SCORE_LABEL_WIDTH, 26));
    scoreLabel.setMinimumSize(new Dimension(SCORE_LABEL_WIDTH, 26));
    scoreLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

    row.add(teamLabel);
    row.add(Box.createHorizontalGlue());
    row.add(scoreLabel);
    return wrapCenteredRow(row);
  }

  private JPanel createScoreRow(int rank, String team, double score) {
    final JPanel row = createCenteredRow();
    row.setOpaque(true);
    row.setBackground(scoreRowColor(rank));
    row.setAlignmentX(Component.CENTER_ALIGNMENT);
    row.setBorder(BorderFactory.createCompoundBorder(
      new LineBorder(SUBTLE_BORDER_COLOR, 1, true),
      BorderFactory.createEmptyBorder(6, 10, 6, 10)
    ));
    row.setMaximumSize(new Dimension(ROW_WIDTH, 34));
    row.setPreferredSize(new Dimension(ROW_WIDTH, 34));

    final JLabel teamLabel = new JLabel("#" + rank + " " + team, SwingConstants.LEFT);
    teamLabel.setFont(teamLabel.getFont().deriveFont(Font.PLAIN, BUTTON_FONT_SIZE + 1));
    teamLabel.setForeground(BUTTON_TEXT_COLOR);
    teamLabel.setPreferredSize(new Dimension(TEAM_LABEL_WIDTH, 22));
    teamLabel.setMinimumSize(new Dimension(TEAM_LABEL_WIDTH, 22));

    final JLabel scoreLabel = new JLabel(formatScore(score), SwingConstants.RIGHT);
    scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, BUTTON_FONT_SIZE + 1));
    scoreLabel.setForeground(BUTTON_TEXT_COLOR);
    scoreLabel.setPreferredSize(new Dimension(SCORE_LABEL_WIDTH, 22));
    scoreLabel.setMinimumSize(new Dimension(SCORE_LABEL_WIDTH, 22));
    scoreLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

    row.add(teamLabel);
    row.add(Box.createHorizontalGlue());
    row.add(scoreLabel);
    return wrapCenteredRow(row);
  }

  private JPanel createEmptyRow() {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.setOpaque(true);
    row.setBackground(new Color(250, 252, 255));
    row.setAlignmentX(Component.CENTER_ALIGNMENT);
    row.setBorder(BorderFactory.createCompoundBorder(
      new LineBorder(SUBTLE_BORDER_COLOR, 1, true),
      BorderFactory.createEmptyBorder(10, 12, 10, 12)
    ));
    row.setMaximumSize(new Dimension(ROW_WIDTH, 40));
    row.setPreferredSize(new Dimension(ROW_WIDTH, 40));

    final JLabel label = new JLabel("No team scores yet. Select winners to populate.");
    label.setFont(label.getFont().deriveFont(Font.PLAIN, BUTTON_FONT_SIZE + 1));
    label.setForeground(BUTTON_TEXT_COLOR);
    row.add(label);
    return wrapCenteredRow(row);
  }

  private String formatScore(double score) {
    if (Math.abs(score - Math.rint(score)) < 0.001) {
      return String.format("%.0f", score);
    }
    return String.format("%.1f", score);
  }

  private JPanel createCenteredRow() {
    final JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.setAlignmentX(Component.CENTER_ALIGNMENT);
    return row;
  }

  private JPanel wrapCenteredRow(JComponent row) {
    final JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    wrapper.setOpaque(false);
    wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
    wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
    wrapper.add(row);
    return wrapper;
  }

  private Color scoreRowColor(int rank) {
    int index = Math.max(0, rank - 1);
    if (index < rowColors.size()) {
      return rowColors.get(index);
    }
    return defaultRowColor;
  }
}
