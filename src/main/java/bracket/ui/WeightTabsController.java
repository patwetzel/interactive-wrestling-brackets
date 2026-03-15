package bracket.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WeightTabsController {

  private static final Color WEIGHT_TAB_ACTIVE_BG = new Color(222, 234, 255);
  private static final Color WEIGHT_TAB_IDLE_BG = new Color(248, 250, 253);
  private static final Color WEIGHT_TAB_ACTIVE_TEXT = new Color(24, 45, 84);
  private static final Color WEIGHT_TAB_IDLE_TEXT = new Color(52, 62, 76);
  private static final Color WEIGHT_TAB_ACTIVE_BORDER = new Color(140, 172, 230);
  private static final Color WEIGHT_TAB_IDLE_BORDER = new Color(205, 214, 228);

  private final JPanel weightTabsPanel;
  private final List<JButton> weightTabButtons = new ArrayList<>();
  private final List<String> weightSheetNames = new ArrayList<>();
  private JButton allAmericansTabButton;
  private JButton teamScoresTabButton;

  public WeightTabsController(JPanel weightTabsPanel) {
    this.weightTabsPanel = weightTabsPanel;
    this.weightTabsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 6));
  }

  public void buildTabs(
    List<String> sheetNames,
    int maxTabs,
    String allAmericansLabel,
    String teamScoresLabel,
    Consumer<String> onSelectSheet,
    Runnable onShowAllAmericans,
    Runnable onShowTeamScores
  ) {
    weightTabsPanel.removeAll();
    weightTabButtons.clear();
    weightSheetNames.clear();

    final int limit = Math.min(maxTabs, sheetNames.size());
    for (int i = 0; i < limit; i++) {
      final String sheetName = sheetNames.get(i);
      final JButton tabButton = new JButton(sheetName);
      tabButton.addActionListener(e -> onSelectSheet.accept(sheetName));
      styleWeightTabButton(tabButton);
      weightTabsPanel.add(tabButton);
      weightTabButtons.add(tabButton);
      weightSheetNames.add(sheetName);
    }

    allAmericansTabButton = new JButton(allAmericansLabel);
    allAmericansTabButton.addActionListener(e -> onShowAllAmericans.run());
    styleWeightTabButton(allAmericansTabButton);
    weightTabsPanel.add(allAmericansTabButton);

    teamScoresTabButton = new JButton(teamScoresLabel);
    teamScoresTabButton.addActionListener(e -> onShowTeamScores.run());
    styleWeightTabButton(teamScoresTabButton);
    weightTabsPanel.add(teamScoresTabButton);

    weightTabsPanel.revalidate();
    weightTabsPanel.repaint();
  }

  public void updateState(String selectedSheetName, boolean showingAllAmericans, boolean showingTeamScores) {
    for (JButton tabButton : weightTabButtons) {
      final boolean selected = tabButton.getText().equals(selectedSheetName);
      tabButton.setEnabled(!selected);
      applyWeightTabState(tabButton, selected);
    }
    if (allAmericansTabButton != null) {
      allAmericansTabButton.setEnabled(!showingAllAmericans);
      applyWeightTabState(allAmericansTabButton, showingAllAmericans);
    }
    if (teamScoresTabButton != null) {
      teamScoresTabButton.setEnabled(!showingTeamScores);
      applyWeightTabState(teamScoresTabButton, showingTeamScores);
    }
  }

  public List<String> getWeightSheetNames() {
    return new ArrayList<>(weightSheetNames);
  }

  private void styleWeightTabButton(JButton button) {
    button.setFocusPainted(false);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
    button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
    applyWeightTabState(button, false);
  }

  private void applyWeightTabState(JButton button, boolean selected) {
    final Color borderColor = selected ? WEIGHT_TAB_ACTIVE_BORDER : WEIGHT_TAB_IDLE_BORDER;
    button.setBorder(BorderFactory.createCompoundBorder(
      new LineBorder(borderColor, 1, true),
      BorderFactory.createEmptyBorder(6, 14, 6, 14)
    ));
    button.setBackground(selected ? WEIGHT_TAB_ACTIVE_BG : WEIGHT_TAB_IDLE_BG);
    button.setForeground(selected ? WEIGHT_TAB_ACTIVE_TEXT : WEIGHT_TAB_IDLE_TEXT);
  }
}
