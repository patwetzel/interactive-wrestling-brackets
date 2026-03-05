package bracket;

import static constants.Constants.*;

import constants.RoundDefinition;
import match.Match;
import match.MatchCardFactory;
import match.MatchNode;
import wrestler.SeededWrestlerImporter;
import wrestler.Wrestler;
import wrestler.WrestlerLabelFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Bracket {

  private final List<Wrestler> wrestlers = new ArrayList<>();
  private final JFrame frame = new JFrame(WINDOW_TITLE);
  private final JPanel buttonPanel = new JPanel();
  private final JPanel weightTabsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
  private final JScrollPane bracketScrollPane = new JScrollPane(buttonPanel);
  private final List<MatchNode> bracketNodes = new ArrayList<>();
  private final List<Wrestler> initialSeededWrestlers = new ArrayList<>();
  private final SeededWrestlerImporter seededWrestlerImporter = new SeededWrestlerImporter();
  private final List<JButton> weightTabButtons = new ArrayList<>();
  private final Map<String, List<Integer>> weightProgressBySheet = new HashMap<>();

  private Path seedingFilePath;
  private String selectedSheetName;

  private BracketBoardPanel bracketBoard;

  public Bracket() {
    setupUI();
    loadInitialSeedingIfPresent();
  }

  private void setupUI() {
    setupFrame();
    setupPanels();
    frame.setVisible(true);
  }

  private void setupFrame() {
    frame.setSize(WINDOW_SIZE);
    frame.setLayout(new BorderLayout());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
  }

  private void setupPanels() {
    final JPanel mainPanel = new JPanel(new BorderLayout());

    buttonPanel.setBorder(BorderFactory.createTitledBorder("Wrestlers"));
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

    bracketScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
    bracketScrollPane.getVerticalScrollBar().setBlockIncrement(SCROLL_BLOCK_INCREMENT);
    bracketScrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
    bracketScrollPane.getHorizontalScrollBar().setBlockIncrement(SCROLL_BLOCK_INCREMENT);
    bracketScrollPane.setWheelScrollingEnabled(true);

    mainPanel.add(bracketScrollPane, BorderLayout.CENTER);
    weightTabsPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
    mainPanel.add(weightTabsPanel, BorderLayout.SOUTH);
    frame.add(mainPanel, BorderLayout.CENTER);
  }

  private void loadInitialSeedingIfPresent() {
    final Optional<Path> seedingPathOpt = resolveSeedingPath();
    if (seedingPathOpt.isEmpty()) {
      JOptionPane.showMessageDialog(
        frame,
        "Could not find a seeding file.\nExpected:\n- src/test/java/SeedingTest.xlsx",
        "Seeding File Missing",
        JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    seedingFilePath = seedingPathOpt.get();

    try {
      final List<String> sheetNames = seededWrestlerImporter.readSheetNames(seedingFilePath);
      buildWeightTabs(sheetNames);
      if (sheetNames.isEmpty()) {
        throw new IllegalStateException("No sheets found in seeding workbook.");
      }
      loadWeightSheet(sheetNames.get(0));
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
        frame,
        "Failed to read seeding file: " + e.getMessage(),
        "Seeding File Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }

  private void buildWeightTabs(List<String> sheetNames) {
    weightTabsPanel.removeAll();
    weightTabButtons.clear();

    final int maxTabs = Math.min(10, sheetNames.size());
    for (int i = 0; i < maxTabs; i++) {
      final String sheetName = sheetNames.get(i);
      final JButton tabButton = new JButton(sheetName);
      tabButton.addActionListener(e -> loadWeightSheet(sheetName));
      weightTabsPanel.add(tabButton);
      weightTabButtons.add(tabButton);
    }

    weightTabsPanel.revalidate();
    weightTabsPanel.repaint();
  }

  private void loadWeightSheet(String sheetName) {
    if (seedingFilePath == null) {
      return;
    }

    if (selectedSheetName != null && !selectedSheetName.equals(sheetName)) {
      saveCurrentWeightProgress(selectedSheetName);
    }

    try {
      final List<Wrestler> seededWrestlers = seededWrestlerImporter.readSeededWrestlers(seedingFilePath, sheetName);
      if (seededWrestlers.size() != 33) {
        JOptionPane.showMessageDialog(
          frame,
          "Expected 33 wrestlers in sheet '" + sheetName + "' but found " + seededWrestlers.size() + ".",
          "Seeding File Error",
          JOptionPane.WARNING_MESSAGE
        );
        return;
      }

      seededWrestlers.sort(Comparator.comparingInt(Wrestler::getSeed));
      wrestlers.clear();
      wrestlers.addAll(seededWrestlers);
      initialSeededWrestlers.clear();
      initialSeededWrestlers.addAll(seededWrestlers);
      selectedSheetName = sheetName;
      updateWeightTabState();
      renderSeededBracket(seededWrestlers);
      restoreWeightProgress(sheetName);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
        frame,
        "Failed to read sheet '" + sheetName + "': " + e.getMessage(),
        "Seeding File Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }

  private void updateWeightTabState() {
    for (JButton tabButton : weightTabButtons) {
      boolean selected = tabButton.getText().equals(selectedSheetName);
      tabButton.setEnabled(!selected);
    }
  }

  private void saveCurrentWeightProgress(String sheetName) {
    if (sheetName == null || bracketNodes.isEmpty()) {
      return;
    }

    List<Integer> winningSlots = new ArrayList<>(bracketNodes.size());
    for (MatchNode node : bracketNodes) {
      winningSlots.add(node.getWinningSlot());
    }
    weightProgressBySheet.put(sheetName, winningSlots);
  }

  private void restoreWeightProgress(String sheetName) {
    List<Integer> winningSlots = weightProgressBySheet.get(sheetName);
    if (winningSlots == null || winningSlots.isEmpty()) {
      return;
    }

    int limit = Math.min(winningSlots.size(), bracketNodes.size());
    for (int i = 0; i < limit; i++) {
      int winningSlot = winningSlots.get(i);
      if (winningSlot == 1 || winningSlot == 2) {
        advanceWinner(bracketNodes.get(i), winningSlot);
      }
    }
  }

  private Optional<Path> resolveSeedingPath() {
    for (Path candidate : SEEDING_FILE_CANDIDATES) {
      final Path absolute = candidate.toAbsolutePath();
      if (Files.exists(absolute)) {
        return Optional.of(absolute);
      }
    }
    return Optional.empty();
  }

  private void renderSeededBracket(List<Wrestler> seededWrestlers) {
    final Map<Integer, Wrestler> bySeed = mapBySeed(seededWrestlers);

    bracketNodes.clear();
    buttonPanel.removeAll();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder());
    buttonPanel.setLayout(new BorderLayout());

    bracketBoard = new BracketBoardPanel(bracketNodes);
    bracketBoard.setLayout(new BoxLayout(bracketBoard, BoxLayout.X_AXIS));
    bracketBoard.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_PANEL_GAP, ROUND_OUTER_PADDING, ROUND_PANEL_GAP));

    final EnumMap<RoundDefinition, List<MatchNode>> rounds = new EnumMap<>(RoundDefinition.class);
    for (RoundDefinition round : RoundDefinition.values()) {
      rounds.put(round, createRoundSection(bracketBoard, round));
    }

    seedOpeningMatches(bySeed, rounds.get(RoundDefinition.PIGTAIL), rounds.get(RoundDefinition.ROUND_OF_32));
    connectRounds(rounds);

    buttonPanel.add(bracketBoard, BorderLayout.NORTH);

    for (MatchNode node : bracketNodes) {
      refreshMatchNode(node);
    }

    buttonPanel.revalidate();
    buttonPanel.repaint();
  }

  private Map<Integer, Wrestler> mapBySeed(List<Wrestler> seededWrestlers) {
    final Map<Integer, Wrestler> bySeed = new HashMap<>();
    for (Wrestler seeded : seededWrestlers) {
      bySeed.put(seeded.getSeed(), seeded);
    }
    return bySeed;
  }

  private void seedOpeningMatches(Map<Integer, Wrestler> bySeed, List<MatchNode> pigtail, List<MatchNode> roundOf32) {
    final Match pigtailMatch = pigtail.get(0).getMatch();
    pigtailMatch.setWrestlerOne(bySeed.get(33));
    pigtailMatch.setWrestlerTwo(bySeed.get(32));

    final Match firstRoundTop = roundOf32.get(0).getMatch();
    firstRoundTop.setWrestlerTwo(bySeed.get(1));

    final int[] highSeedOrder = {16, 8, 9, 5, 12, 13, 4, 3, 14, 11, 6, 7, 10, 15, 2};
    for (int i = 1; i < roundOf32.size(); i++) {
      final int highSeed = highSeedOrder[i - 1];
      final int lowSeed = 33 - highSeed;
      final Match roundMatch = roundOf32.get(i).getMatch();
      roundMatch.setWrestlerOne(bySeed.get(highSeed));
      roundMatch.setWrestlerTwo(bySeed.get(lowSeed));
    }
  }

  private void connectRounds(EnumMap<RoundDefinition, List<MatchNode>> rounds) {
    BracketConnector.connectWinner(rounds.get(RoundDefinition.PIGTAIL).get(0), rounds.get(RoundDefinition.ROUND_OF_32).get(0), 1);
    BracketConnector.connectRounds(rounds.get(RoundDefinition.ROUND_OF_32), rounds.get(RoundDefinition.ROUND_OF_16));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.ROUND_OF_16), rounds.get(RoundDefinition.QUARTERFINALS));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.QUARTERFINALS), rounds.get(RoundDefinition.SEMIFINALS));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.SEMIFINALS), rounds.get(RoundDefinition.FINAL));
  }

  private List<MatchNode> createRoundSection(JPanel bracketPanel, RoundDefinition round) {
    final JPanel roundPanel = new JPanel();
    roundPanel.setLayout(new BoxLayout(roundPanel, BoxLayout.Y_AXIS));
    roundPanel.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    roundPanel.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    roundPanel.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    addRoundHeader(roundPanel, round.getDisplayName());
    if (round.isResetButton()) {
      addResetButtonToFinalRound(roundPanel);
      roundPanel.add(Box.createVerticalStrut(ROUND_HEADER_BOTTOM_SPACING));
    }

    final int topOffset = BracketLayout.calculateTopOffset(round.getRoundDepth());
    final int betweenGap = BracketLayout.calculateBetweenGap(round.getRoundDepth());
    if (topOffset > 0) {
      roundPanel.add(Box.createVerticalStrut(topOffset));
    }

    final List<MatchNode> nodes = new ArrayList<>();
    for (int i = 0; i < round.getMatchCount(); i++) {
      final Match match = new Match();

      final JButton buttonOne = MatchCardFactory.createWrestlerButton();
      final JButton buttonTwo = MatchCardFactory.createWrestlerButton();
      final JPanel matchCard = MatchCardFactory.createMatchCard();

      matchCard.add(buttonOne);
      matchCard.add(Box.createVerticalStrut(MATCH_BUTTON_GAP));
      matchCard.add(buttonTwo);

      roundPanel.add(matchCard);
      if (i < round.getMatchCount() - 1) {
        roundPanel.add(Box.createVerticalStrut(betweenGap));
      }

      final MatchNode node = new MatchNode(match, buttonOne, buttonTwo, matchCard);
      buttonOne.addActionListener(e -> advanceWinner(node, 1));
      buttonTwo.addActionListener(e -> advanceWinner(node, 2));

      nodes.add(node);
      bracketNodes.add(node);
    }

    bracketPanel.add(roundPanel);
    bracketPanel.add(Box.createHorizontalStrut(ROUND_PANEL_GAP));
    return nodes;
  }

  private void addRoundHeader(JPanel roundPanel, String roundName) {
    final JLabel header = new JLabel(roundName + ":", SwingConstants.CENTER);
    header.setFont(header.getFont().deriveFont(Font.BOLD, ROUND_HEADER_FONT_SIZE));

    final JPanel headerRow = new JPanel(new BorderLayout());
    headerRow.setOpaque(false);
    headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROUND_HEADER_HEIGHT));
    headerRow.add(header, BorderLayout.CENTER);

    roundPanel.add(headerRow);
    roundPanel.add(Box.createVerticalStrut(ROUND_HEADER_BOTTOM_SPACING));
  }

  private void addResetButtonToFinalRound(JPanel roundPanel) {
    final JButton resetButton = new JButton("Reset Bracket");
    resetButton.addActionListener(e -> {
      if (!initialSeededWrestlers.isEmpty()) {
        if (selectedSheetName != null) {
          weightProgressBySheet.remove(selectedSheetName);
        }
        renderSeededBracket(initialSeededWrestlers);
      }
    });

    final JPanel centeredRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    centeredRow.setOpaque(false);
    centeredRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    centeredRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, resetButton.getPreferredSize().height));
    centeredRow.add(resetButton);
    roundPanel.add(centeredRow);
  }

  private void advanceWinner(MatchNode source, int winningSlot) {
    final Wrestler wrestlerOne = source.getMatch().getWrestlerOne();
    final Wrestler wrestlerTwo = source.getMatch().getWrestlerTwo();
    if (wrestlerOne == null || wrestlerTwo == null) {
      return;
    }

    final boolean winnerChanged = source.isCompleted() && source.getWinningSlot() != winningSlot;
    if (winnerChanged) {
      resetDecisionsFrom(source.getNextMatch());
    }

    source.markWinner(winningSlot);
    refreshMatchNode(source);

    final MatchNode nextMatch = source.getNextMatch();
    if (nextMatch == null) {
      repaintBracketBoard();
      return;
    }

    final Wrestler winner = source.selectedWinner();
    if (source.getNextSlot() == 1) {
      nextMatch.getMatch().setWrestlerOne(winner);
    } else {
      nextMatch.getMatch().setWrestlerTwo(winner);
    }

    refreshMatchNode(nextMatch);
    repaintBracketBoard();
  }

  private void resetDecisionsFrom(MatchNode node) {
    if (node == null) {
      return;
    }

    node.clearDecision();

    final MatchNode next = node.getNextMatch();
    if (next != null) {
      if (node.getNextSlot() == 1) {
        next.getMatch().setWrestlerOne(null);
      } else {
        next.getMatch().setWrestlerTwo(null);
      }
      resetDecisionsFrom(next);
      refreshMatchNode(next);
    }

    refreshMatchNode(node);
  }

  private void refreshMatchNode(MatchNode node) {
    final Wrestler wrestlerOne = node.getMatch().getWrestlerOne();
    final Wrestler wrestlerTwo = node.getMatch().getWrestlerTwo();

    node.getButtonOne().setText(WrestlerLabelFormatter.format(wrestlerOne));
    node.getButtonTwo().setText(WrestlerLabelFormatter.format(wrestlerTwo));

    final boolean canWrestle = wrestlerOne != null && wrestlerTwo != null;
    node.getButtonOne().setEnabled(canWrestle);
    node.getButtonTwo().setEnabled(canWrestle);

    BracketUiStyler.resetButtonColors(node);
    if (node.isCompleted()) {
      BracketUiStyler.applyResultColors(node);
    }
  }

  private void repaintBracketBoard() {
    if (bracketBoard != null) {
      bracketBoard.repaint();
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(Bracket::new);
  }
}
