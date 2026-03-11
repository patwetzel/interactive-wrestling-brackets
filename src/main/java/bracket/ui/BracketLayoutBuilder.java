package bracket.ui;

import static constants.Constants.*;

import bracket.BracketBoardPanel;
import bracket.BracketLayout;
import constants.BracketSection;
import constants.RoundDefinition;
import match.MatchNode;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;

public final class BracketLayoutBuilder {

  @FunctionalInterface
  public interface MatchNodeFactory {
    MatchNode create(BracketSection section);
  }

  public static final class LayoutResult {
    private final BracketBoardPanel board;
    private final EnumMap<RoundDefinition, List<MatchNode>> rounds;
    private final EnumMap<RoundDefinition, List<MatchNode>> consolationRounds;
    private final EnumMap<RoundDefinition, MatchNode> placementMatches;

    private LayoutResult(
      BracketBoardPanel board,
      EnumMap<RoundDefinition, List<MatchNode>> rounds,
      EnumMap<RoundDefinition, List<MatchNode>> consolationRounds,
      EnumMap<RoundDefinition, MatchNode> placementMatches
    ) {
      this.board = board;
      this.rounds = rounds;
      this.consolationRounds = consolationRounds;
      this.placementMatches = placementMatches;
    }

    public BracketBoardPanel board() {
      return board;
    }

    public EnumMap<RoundDefinition, List<MatchNode>> rounds() {
      return rounds;
    }

    public EnumMap<RoundDefinition, List<MatchNode>> consolationRounds() {
      return consolationRounds;
    }

    public EnumMap<RoundDefinition, MatchNode> placementMatches() {
      return placementMatches;
    }
  }

  private static final RoundDefinition[] CONSOLATION_ROUNDS = {
    RoundDefinition.CONSOLATION_PIGTAIL,
    RoundDefinition.CONSOLATION_ROUND_1,
    RoundDefinition.CONSOLATION_ROUND_2,
    RoundDefinition.CONSOLATION_ROUND_3,
    RoundDefinition.BLOOD_ROUND,
    RoundDefinition.BLOOD_ROUND_WINNERS,
    RoundDefinition.CONSOLATION_SEMIFINALS,
    RoundDefinition.THIRD_PLACE
  };

  private final MatchNodeFactory matchNodeFactory;
  private final Runnable openAction;
  private final Runnable saveAction;
  private final Runnable resetAction;
  private final AllAmericansPanelBuilder allAmericansPanelBuilder;

  public BracketLayoutBuilder(
    MatchNodeFactory matchNodeFactory,
    Runnable openAction,
    Runnable saveAction,
    Runnable resetAction,
    AllAmericansPanelBuilder allAmericansPanelBuilder
  ) {
    this.matchNodeFactory = matchNodeFactory;
    this.openAction = openAction;
    this.saveAction = saveAction;
    this.resetAction = resetAction;
    this.allAmericansPanelBuilder = allAmericansPanelBuilder;
  }

  public LayoutResult buildLayout(List<MatchNode> bracketNodes, List<JLabel> allAmericanNodes) {
    final BracketBoardPanel board = createBracketBoard(bracketNodes);

    final JPanel championshipRow = createBracketRow();
    final JPanel consolationRow = createBracketRow();

    board.add(championshipRow);
    board.add(Box.createVerticalStrut(BRACKET_ROWS_VERTICAL_GAP));
    board.add(consolationRow);

    final EnumMap<RoundDefinition, List<MatchNode>> rounds = buildChampionshipRounds(championshipRow, bracketNodes);
    final EnumMap<RoundDefinition, List<MatchNode>> consolationRounds = buildConsolationRounds(consolationRow, bracketNodes);
    addResetButtonColumn(championshipRow);
    final EnumMap<RoundDefinition, MatchNode> placementMatches = createPlacementStackSection(
      championshipRow,
      bracketNodes,
      allAmericanNodes
    );

    return new LayoutResult(board, rounds, consolationRounds, placementMatches);
  }

  private BracketBoardPanel createBracketBoard(List<MatchNode> bracketNodes) {
    final BracketBoardPanel board = new BracketBoardPanel(bracketNodes);
    board.setLayout(new BoxLayout(board, BoxLayout.Y_AXIS));
    board.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_PANEL_GAP, ROUND_OUTER_PADDING, ROUND_PANEL_GAP));
    board.setOpaque(false);
    return board;
  }

  private JPanel createBracketRow() {
    final JPanel row = new JPanel();
    row.setOpaque(false);
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
    row.setAlignmentX(Component.LEFT_ALIGNMENT);
    return row;
  }

  private EnumMap<RoundDefinition, List<MatchNode>> buildChampionshipRounds(JPanel championshipRow, List<MatchNode> bracketNodes) {
    final EnumMap<RoundDefinition, List<MatchNode>> rounds = new EnumMap<>(RoundDefinition.class);
    for (RoundDefinition round : RoundDefinition.championshipRounds()) {
      rounds.put(round, createRoundSection(championshipRow, round, bracketNodes));
    }
    return rounds;
  }

  private EnumMap<RoundDefinition, List<MatchNode>> buildConsolationRounds(JPanel consolationRow, List<MatchNode> bracketNodes) {
    final EnumMap<RoundDefinition, List<MatchNode>> consolationRounds = new EnumMap<>(RoundDefinition.class);
    for (RoundDefinition round : CONSOLATION_ROUNDS) {
      consolationRounds.put(round, createConsolationRoundSection(consolationRow, round, bracketNodes));
    }
    return consolationRounds;
  }

  private EnumMap<RoundDefinition, MatchNode> createPlacementStackSection(
    JPanel bracketPanel,
    List<MatchNode> bracketNodes,
    List<JLabel> allAmericanNodes
  ) {
    final JPanel placementPanel = new JPanel();
    placementPanel.setLayout(new BoxLayout(placementPanel, BoxLayout.Y_AXIS));
    placementPanel.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    placementPanel.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    placementPanel.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    placementPanel.add(allAmericansPanelBuilder.buildPlacementSection(allAmericanNodes));
    placementPanel.add(Box.createVerticalStrut(12));
    placementPanel.add(Box.createVerticalGlue());

    MatchNode fifthPlace = createStandalonePlacementMatchNode(bracketNodes);
    placementPanel.add(createPlacementLabeledNode(RoundDefinition.FIFTH_PLACE.getDisplayName(), fifthPlace));
    placementPanel.add(Box.createVerticalStrut(24));
    MatchNode seventhPlace = createStandalonePlacementMatchNode(bracketNodes);
    placementPanel.add(createPlacementLabeledNode(RoundDefinition.SEVENTH_PLACE.getDisplayName(), seventhPlace));

    bracketPanel.add(placementPanel);

    EnumMap<RoundDefinition, MatchNode> placementMatches = new EnumMap<>(RoundDefinition.class);
    placementMatches.put(RoundDefinition.FIFTH_PLACE, fifthPlace);
    placementMatches.put(RoundDefinition.SEVENTH_PLACE, seventhPlace);
    return placementMatches;
  }

  private JPanel createPlacementLabeledNode(String label, MatchNode placementNode) {
    final JPanel wrapper = new JPanel();
    wrapper.setOpaque(false);
    wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
    wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
    wrapper.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, MATCH_CARD_HEIGHT + ROUND_HEADER_HEIGHT + 8));

    final JLabel title = new JLabel(label, SwingConstants.CENTER);
    title.setFont(title.getFont().deriveFont(Font.BOLD, ROUND_HEADER_FONT_SIZE));
    final JPanel titleRow = new JPanel(new BorderLayout());
    titleRow.setOpaque(false);
    titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROUND_HEADER_HEIGHT));
    titleRow.add(title, BorderLayout.CENTER);
    wrapper.add(titleRow);
    wrapper.add(Box.createVerticalStrut(4));

    final JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    cardRow.setOpaque(false);
    cardRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    cardRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, MATCH_CARD_HEIGHT));
    cardRow.add(placementNode.getMatchCard());
    wrapper.add(cardRow);
    return wrapper;
  }

  private MatchNode createStandalonePlacementMatchNode(List<MatchNode> bracketNodes) {
    final MatchNode node = matchNodeFactory.create(BracketSection.PLACEMENT);
    bracketNodes.add(node);
    return node;
  }

  private List<MatchNode> createRoundSection(
    JPanel bracketPanel,
    RoundDefinition round,
    List<MatchNode> bracketNodes
  ) {
    return createRoundSection(
      bracketPanel,
      round.getDisplayName(),
      round.getMatchCount(),
      round.getRoundDepth(),
      round.getBracketSection(),
      bracketNodes
    );
  }

  private List<MatchNode> createConsolationRoundSection(
    JPanel bracketPanel,
    RoundDefinition round,
    List<MatchNode> bracketNodes
  ) {
    final boolean isPlacement = round.isPlacementRound();
    final BracketSection section = round.getBracketSection();
    final int roundDepth = isPlacement ? 0 : round.getRoundDepth();
    return createRoundSection(bracketPanel, round.getDisplayName(), round.getMatchCount(), roundDepth, section, bracketNodes);
  }

  private List<MatchNode> createRoundSection(
    JPanel bracketPanel,
    String roundName,
    int matchCount,
    int roundDepth,
    BracketSection bracketSection,
    List<MatchNode> bracketNodes
  ) {
    final JPanel roundPanel = new JPanel();
    roundPanel.setLayout(new BoxLayout(roundPanel, BoxLayout.Y_AXIS));
    roundPanel.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    roundPanel.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    roundPanel.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    addRoundHeader(roundPanel, roundName);
    final int topOffset = BracketLayout.calculateTopOffset(roundDepth);
    final int betweenGap = BracketLayout.calculateBetweenGap(roundDepth);
    if (topOffset > 0) {
      roundPanel.add(Box.createVerticalStrut(topOffset));
    }

    final List<MatchNode> nodes = new ArrayList<>();
    for (int i = 0; i < matchCount; i++) {
      final MatchNode node = matchNodeFactory.create(bracketSection);
      bracketNodes.add(node);
      nodes.add(node);
      roundPanel.add(node.getMatchCard());
      if (i < matchCount - 1) {
        roundPanel.add(Box.createVerticalStrut(betweenGap));
      }
    }

    final Dimension preferred = roundPanel.getPreferredSize();
    roundPanel.setPreferredSize(new Dimension(ROUND_COLUMN_WIDTH, preferred.height));

    bracketPanel.add(roundPanel);
    bracketPanel.add(Box.createHorizontalStrut(ROUND_PANEL_GAP));
    return nodes;
  }

  private void addRoundHeader(JPanel roundPanel, String roundName) {
    final JLabel header = new JLabel(roundName + ":", SwingConstants.CENTER);
    header.setFont(header.getFont().deriveFont(Font.BOLD, ROUND_HEADER_FONT_SIZE));
    header.setForeground(BUTTON_TEXT_COLOR);

    final JPanel headerRow = new JPanel(new BorderLayout());
    headerRow.setOpaque(false);
    headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROUND_HEADER_HEIGHT));
    headerRow.add(header, BorderLayout.CENTER);

    roundPanel.add(headerRow);
    roundPanel.add(Box.createVerticalStrut(ROUND_HEADER_BOTTOM_SPACING));
  }

  private void addResetButtonColumn(JPanel championshipRow) {
    final JPanel resetColumn = new JPanel();
    resetColumn.setLayout(new BoxLayout(resetColumn, BoxLayout.Y_AXIS));
    resetColumn.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    resetColumn.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    resetColumn.setPreferredSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    resetColumn.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    addResetButtonToFinalRound(resetColumn);
    resetColumn.add(Box.createVerticalGlue());

    championshipRow.add(resetColumn);
    championshipRow.add(Box.createHorizontalStrut(ROUND_PANEL_GAP));
  }

  private void addResetButtonToFinalRound(JPanel roundPanel) {
    final JButton openButton = new JButton("Open Brackets");
    styleActionButton(openButton);
    openButton.addActionListener(e -> openAction.run());
    final JPanel openRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    openRow.setOpaque(false);
    openRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    openRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, openButton.getPreferredSize().height));
    openRow.add(openButton);
    roundPanel.add(openRow);
    roundPanel.add(Box.createVerticalStrut(6));

    final JButton saveButton = new JButton("Save Brackets");
    styleActionButton(saveButton);
    saveButton.addActionListener(e -> saveAction.run());
    final JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    saveRow.setOpaque(false);
    saveRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    saveRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, saveButton.getPreferredSize().height));
    saveRow.add(saveButton);
    roundPanel.add(saveRow);
    roundPanel.add(Box.createVerticalStrut(6));

    final JButton resetButton = new JButton("Reset Bracket");
    styleActionButton(resetButton);
    resetButton.addActionListener(e -> resetAction.run());

    final JPanel centeredRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    centeredRow.setOpaque(false);
    centeredRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    centeredRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, resetButton.getPreferredSize().height));
    centeredRow.add(resetButton);
    roundPanel.add(centeredRow);
  }

  private void styleActionButton(JButton button) {
    button.setFocusPainted(false);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
    button.setBorder(BorderFactory.createCompoundBorder(
      new javax.swing.border.LineBorder(SUBTLE_BORDER_COLOR, 1, true),
      BorderFactory.createEmptyBorder(4, 12, 4, 12)
    ));
    button.setBackground(TAB_IDLE_COLOR);
    button.setForeground(TAB_IDLE_TEXT_COLOR);
  }
}
