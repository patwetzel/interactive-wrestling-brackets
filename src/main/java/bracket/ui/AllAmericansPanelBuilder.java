package bracket.ui;

import static constants.Constants.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

public final class AllAmericansPanelBuilder {

  public static final class OverviewResult {
    private final JPanel panel;
    private final Map<String, List<JLabel>> nodesBySheet;

    private OverviewResult(JPanel panel, Map<String, List<JLabel>> nodesBySheet) {
      this.panel = panel;
      this.nodesBySheet = nodesBySheet;
    }

    public JPanel panel() {
      return panel;
    }

    public Map<String, List<JLabel>> nodesBySheet() {
      return nodesBySheet;
    }
  }

  private final int allAmericanCount;
  private final Color firstColor;
  private final Color secondColor;
  private final Color thirdColor;
  private final Color otherColor;
  private final String headerText;

  public AllAmericansPanelBuilder(
    int allAmericanCount,
    Color firstColor,
    Color secondColor,
    Color thirdColor,
    Color otherColor,
    String headerText
  ) {
    this.allAmericanCount = allAmericanCount;
    this.firstColor = firstColor;
    this.secondColor = secondColor;
    this.thirdColor = thirdColor;
    this.otherColor = otherColor;
    this.headerText = headerText;
  }

  public JPanel buildPlacementSection(List<JLabel> allAmericanNodes) {
    allAmericanNodes.clear();

    final JPanel section = new JPanel();
    section.setOpaque(false);
    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
    section.setAlignmentX(Component.CENTER_ALIGNMENT);
    section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

    final JLabel header = new JLabel(headerText, SwingConstants.CENTER);
    header.setFont(header.getFont().deriveFont(Font.BOLD, ROUND_HEADER_FONT_SIZE));
    header.setForeground(BUTTON_TEXT_COLOR);
    final JPanel headerRow = new JPanel(new BorderLayout());
    headerRow.setOpaque(false);
    headerRow.setAlignmentX(Component.CENTER_ALIGNMENT);
    headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROUND_HEADER_HEIGHT));
    headerRow.add(header, BorderLayout.CENTER);
    section.add(headerRow);
    section.add(Box.createVerticalStrut(6));

    appendAllAmericanRows(section, allAmericanNodes);

    return section;
  }

  public OverviewResult buildOverviewPanel(List<String> weightSheetNames) {
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

    final JPanel topRow = createAllAmericansRow();
    final JPanel bottomRow = createAllAmericansRow();

    final Map<String, List<JLabel>> nodesBySheet = new HashMap<>();
    final int splitIndex = Math.min(5, weightSheetNames.size());
    for (int i = 0; i < weightSheetNames.size(); i++) {
      final String sheetName = weightSheetNames.get(i);
      final JPanel column = createAllAmericansWeightColumn(sheetName, nodesBySheet);
      if (i < splitIndex) {
        topRow.add(column);
        if (i < splitIndex - 1) {
          topRow.add(Box.createHorizontalStrut(ROUND_PANEL_GAP));
        }
      } else {
        bottomRow.add(column);
        if (i < weightSheetNames.size() - 1) {
          bottomRow.add(Box.createHorizontalStrut(ROUND_PANEL_GAP));
        }
      }
    }

    container.add(topRow);
    container.add(Box.createVerticalStrut(18));
    container.add(bottomRow);

    return new OverviewResult(container, nodesBySheet);
  }

  public void refreshOverviewNodes(
    Map<String, List<JLabel>> nodesBySheet,
    Map<String, List<String>> labelsBySheet,
    IntFunction<String> defaultLabelSupplier
  ) {
    for (Map.Entry<String, List<JLabel>> entry : nodesBySheet.entrySet()) {
      final String sheetName = entry.getKey();
      final List<JLabel> nodes = entry.getValue();
      final List<String> placements = labelsBySheet.get(sheetName);

      for (int i = 0; i < nodes.size(); i++) {
        final String label = placements != null && i < placements.size()
          ? placements.get(i)
          : defaultLabelSupplier.apply(i + 1);
        nodes.get(i).setText(label);
      }
    }
  }

  private JPanel createAllAmericansRow() {
    final JPanel row = new JPanel();
    row.setOpaque(false);
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.setAlignmentX(Component.CENTER_ALIGNMENT);
    return row;
  }

  private JPanel createAllAmericansWeightColumn(String sheetName, Map<String, List<JLabel>> nodesBySheet) {
    final JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setAlignmentX(Component.CENTER_ALIGNMENT);
    column.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    column.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    column.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    final JLabel title = new JLabel(sheetName, SwingConstants.CENTER);
    title.setFont(title.getFont().deriveFont(Font.BOLD, ROUND_HEADER_FONT_SIZE));
    title.setForeground(BUTTON_TEXT_COLOR);
    final JPanel titleRow = new JPanel(new BorderLayout());
    titleRow.setOpaque(false);
    titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);
    final Dimension titleSize = new Dimension(ROUND_COLUMN_WIDTH, ROUND_HEADER_HEIGHT);
    titleRow.setMinimumSize(titleSize);
    titleRow.setPreferredSize(titleSize);
    titleRow.setMaximumSize(titleSize);
    titleRow.add(title, BorderLayout.CENTER);
    column.add(titleRow);
    column.add(Box.createVerticalStrut(6));

    final List<JLabel> nodes = new ArrayList<>();
    appendAllAmericanRows(column, nodes);

    nodesBySheet.put(sheetName, nodes);
    return column;
  }

  private void appendAllAmericanRows(JPanel parent, List<JLabel> nodes) {
    for (int i = 1; i <= allAmericanCount; i++) {
      final JPanel row = new JPanel(new BorderLayout());
      row.setOpaque(false);
      row.setAlignmentX(Component.CENTER_ALIGNMENT);
      row.setPreferredSize(new Dimension(ROUND_COLUMN_WIDTH, WRESTLER_BUTTON_SIZE.height));
      row.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, WRESTLER_BUTTON_SIZE.height));

      final JLabel node = createAllAmericanNode(i);
      nodes.add(node);
      row.add(node, BorderLayout.CENTER);
      parent.add(row);
      if (i < allAmericanCount) {
        parent.add(Box.createVerticalStrut(4));
      }
    }
  }

  private JLabel createAllAmericanNode(int placement) {
    final JLabel node = new JLabel("#" + placement + " TBD", SwingConstants.CENTER);
    node.setAlignmentX(Component.CENTER_ALIGNMENT);
    node.setHorizontalAlignment(SwingConstants.CENTER);
    node.setHorizontalTextPosition(SwingConstants.CENTER);
    node.setOpaque(true);
    node.setBackground(allAmericanPlacementColor(placement));
    node.setForeground(BUTTON_TEXT_COLOR);
    node.setFont(node.getFont().deriveFont(Font.PLAIN, BUTTON_FONT_SIZE));
    node.setBorder(BorderFactory.createCompoundBorder(
      new LineBorder(SUBTLE_BORDER_COLOR, 1, true),
      BorderFactory.createEmptyBorder(2, 4, 2, 4)
    ));
    final Dimension allAmericanNodeSize = new Dimension(
      ROUND_COLUMN_WIDTH,
      WRESTLER_BUTTON_SIZE.height
    );
    node.setPreferredSize(allAmericanNodeSize);
    node.setMinimumSize(allAmericanNodeSize);
    node.setMaximumSize(allAmericanNodeSize);
    return node;
  }

  private Color allAmericanPlacementColor(int placement) {
    if (placement == 1) {
      return firstColor;
    }
    if (placement == 2) {
      return secondColor;
    }
    if (placement == 3) {
      return thirdColor;
    }
    return otherColor;
  }
}
