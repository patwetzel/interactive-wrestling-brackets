package bracket;

import static constants.Constants.*;

import constants.RoundDefinition;
import constants.BracketSection;
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

  private static final int[][] ROUND_OF_32_SEED_ORDER = {
    {17, 16},
    {9, 24},
    {25, 8},
    {5, 28},
    {21, 12},
    {13, 20},
    {29, 4},
    {3, 30},
    {19, 14},
    {11, 22},
    {27, 6},
    {7, 26},
    {23, 10},
    {15, 18},
    {31, 2}
  };

  private static final int THREE_VS_THIRTY_ROUND_OF_32_INDEX = 8;

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
    bracketBoard.setLayout(new BoxLayout(bracketBoard, BoxLayout.Y_AXIS));
    bracketBoard.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_PANEL_GAP, ROUND_OUTER_PADDING, ROUND_PANEL_GAP));

    final JPanel championshipRow = new JPanel();
    championshipRow.setOpaque(false);
    championshipRow.setLayout(new BoxLayout(championshipRow, BoxLayout.X_AXIS));
    championshipRow.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JPanel consolationRow = new JPanel();
    consolationRow.setOpaque(false);
    consolationRow.setLayout(new BoxLayout(consolationRow, BoxLayout.X_AXIS));
    consolationRow.setAlignmentX(Component.LEFT_ALIGNMENT);

    bracketBoard.add(championshipRow);
    bracketBoard.add(Box.createVerticalStrut(80));
    bracketBoard.add(consolationRow);

    final EnumMap<RoundDefinition, List<MatchNode>> rounds = new EnumMap<>(RoundDefinition.class);
    for (RoundDefinition round : RoundDefinition.championshipRounds()) {
      rounds.put(round, createRoundSection(championshipRow, round));
    }

    final EnumMap<RoundDefinition, List<MatchNode>> consolationRounds = new EnumMap<>(RoundDefinition.class);
    for (RoundDefinition round : CONSOLATION_ROUNDS) {
      consolationRounds.put(round, createConsolationRoundSection(consolationRow, round));
    }
    final EnumMap<RoundDefinition, MatchNode> placementMatches = createPlacementStackSection(championshipRow);

    seedOpeningMatches(bySeed, rounds.get(RoundDefinition.PIGTAIL), rounds.get(RoundDefinition.ROUND_OF_32));
    connectRounds(rounds, consolationRounds, placementMatches);

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

    for (int i = 1; i < roundOf32.size(); i++) {
      final int firstSeed = ROUND_OF_32_SEED_ORDER[i - 1][0];
      final int secondSeed = ROUND_OF_32_SEED_ORDER[i - 1][1];
      final Match roundMatch = roundOf32.get(i).getMatch();
      roundMatch.setWrestlerOne(bySeed.get(firstSeed));
      roundMatch.setWrestlerTwo(bySeed.get(secondSeed));
    }
  }

  private void connectRounds(
    EnumMap<RoundDefinition, List<MatchNode>> rounds,
    EnumMap<RoundDefinition, List<MatchNode>> consolationRounds,
    EnumMap<RoundDefinition, MatchNode> placementMatches
  ) {
    BracketConnector.connectWinner(rounds.get(RoundDefinition.PIGTAIL).get(0), rounds.get(RoundDefinition.ROUND_OF_32).get(0), 1);
    BracketConnector.connectRounds(rounds.get(RoundDefinition.ROUND_OF_32), rounds.get(RoundDefinition.ROUND_OF_16));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.ROUND_OF_16), rounds.get(RoundDefinition.QUARTERFINALS));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.QUARTERFINALS), rounds.get(RoundDefinition.SEMIFINALS));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.SEMIFINALS), rounds.get(RoundDefinition.FINAL));
    connectConsolationPath(rounds, consolationRounds, placementMatches);
  }

  private void connectConsolationPath(
    EnumMap<RoundDefinition, List<MatchNode>> rounds,
    EnumMap<RoundDefinition, List<MatchNode>> consolationRounds,
    EnumMap<RoundDefinition, MatchNode> placementMatches
  ) {
    final List<MatchNode> roundOf32 = rounds.get(RoundDefinition.ROUND_OF_32);
    final List<MatchNode> roundOf16 = rounds.get(RoundDefinition.ROUND_OF_16);
    final List<MatchNode> quarterfinals = rounds.get(RoundDefinition.QUARTERFINALS);
    final List<MatchNode> semifinals = rounds.get(RoundDefinition.SEMIFINALS);

    final MatchNode consiPigtail = consolationRounds.get(RoundDefinition.CONSOLATION_PIGTAIL).get(0);
    final List<MatchNode> consRound1 = consolationRounds.get(RoundDefinition.CONSOLATION_ROUND_1);
    final List<MatchNode> consRound2 = consolationRounds.get(RoundDefinition.CONSOLATION_ROUND_2);
    final List<MatchNode> consRound3 = consolationRounds.get(RoundDefinition.CONSOLATION_ROUND_3);
    final List<MatchNode> bloodRound = consolationRounds.get(RoundDefinition.BLOOD_ROUND);
    final List<MatchNode> bloodRoundWinners = consolationRounds.get(RoundDefinition.BLOOD_ROUND_WINNERS);
    final List<MatchNode> consSemifinals = consolationRounds.get(RoundDefinition.CONSOLATION_SEMIFINALS);
    final MatchNode thirdPlace = consolationRounds.get(RoundDefinition.THIRD_PLACE).get(0);
    final MatchNode fifthPlace = placementMatches.get(RoundDefinition.FIFTH_PLACE);
    final MatchNode seventhPlace = placementMatches.get(RoundDefinition.SEVENTH_PLACE);

    BracketConnector.connectLoser(rounds.get(RoundDefinition.PIGTAIL).get(0), consiPigtail, 1);
    BracketConnector.connectLoser(roundOf32.get(THREE_VS_THIRTY_ROUND_OF_32_INDEX), consiPigtail, 2);
    BracketConnector.connectWinner(consiPigtail, consRound1.get(4), 1);

    for (int i = 0; i < roundOf32.size(); i++) {
      if (i == THREE_VS_THIRTY_ROUND_OF_32_INDEX) {
        continue;
      }
      BracketConnector.connectLoser(roundOf32.get(i), consRound1.get(i / 2), (i % 2) + 1);
    }

    for (int i = 0; i < consRound1.size(); i++) {
      BracketConnector.connectWinner(consRound1.get(i), consRound2.get(i), 1);
    }
    for (int i = 0; i < roundOf16.size(); i++) {
      BracketConnector.connectLoser(roundOf16.get(i), consRound2.get(i), 2);
    }

    BracketConnector.connectRounds(consRound2, consRound3);

    for (int i = 0; i < consRound3.size(); i++) {
      BracketConnector.connectWinner(consRound3.get(i), bloodRound.get(i), 1);
    }
    for (int i = 0; i < quarterfinals.size(); i++) {
      BracketConnector.connectLoser(quarterfinals.get(i), bloodRound.get(i), 2);
    }

    BracketConnector.connectRounds(bloodRound, bloodRoundWinners);

    for (int i = 0; i < bloodRoundWinners.size(); i++) {
      BracketConnector.connectWinner(bloodRoundWinners.get(i), consSemifinals.get(i), 2);
      BracketConnector.connectLoser(bloodRoundWinners.get(i), seventhPlace, i + 1);
    }

    for (int i = 0; i < semifinals.size(); i++) {
      BracketConnector.connectLoser(semifinals.get(i), consSemifinals.get(i), 1);
    }

    for (int i = 0; i < consSemifinals.size(); i++) {
      BracketConnector.connectWinner(consSemifinals.get(i), thirdPlace, (i % 2) + 1);
      BracketConnector.connectLoser(consSemifinals.get(i), fifthPlace, (i % 2) + 1);
    }
  }

  private List<MatchNode> createRoundSection(JPanel bracketPanel, RoundDefinition round) {
    return createRoundSection(
      bracketPanel,
      round.getDisplayName(),
      round.getMatchCount(),
      round.getRoundDepth(),
      round.isResetButton(),
      round.getBracketSection()
    );
  }

  private List<MatchNode> createConsolationRoundSection(JPanel bracketPanel, RoundDefinition round) {
    boolean isPlacement = round.isPlacementRound();
    BracketSection section = round.getBracketSection();
    int roundDepth = isPlacement ? 0 : round.getRoundDepth();
    return createRoundSection(bracketPanel, round.getDisplayName(), round.getMatchCount(), roundDepth, false, section);
  }

  private EnumMap<RoundDefinition, MatchNode> createPlacementStackSection(JPanel bracketPanel) {
    final JPanel placementPanel = new JPanel();
    placementPanel.setLayout(new BoxLayout(placementPanel, BoxLayout.Y_AXIS));
    placementPanel.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    placementPanel.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    placementPanel.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    addRoundHeader(placementPanel, "Placement");

    int semiDepth = RoundDefinition.SEMIFINALS.getRoundDepth();
    int topOffset = BracketLayout.calculateTopOffset(semiDepth);
    int betweenGap = BracketLayout.calculateBetweenGap(semiDepth);
    if (topOffset > 0) {
      placementPanel.add(Box.createVerticalStrut(topOffset));
    }

    MatchNode fifthPlace = createStandalonePlacementMatchNode();
    placementPanel.add(createPlacementLabeledNode(RoundDefinition.FIFTH_PLACE.getDisplayName(), fifthPlace));
    placementPanel.add(Box.createVerticalStrut(Math.max(24, betweenGap - 24)));
    MatchNode seventhPlace = createStandalonePlacementMatchNode();
    placementPanel.add(createPlacementLabeledNode(RoundDefinition.SEVENTH_PLACE.getDisplayName(), seventhPlace));

    bracketPanel.add(placementPanel);
    bracketPanel.add(Box.createHorizontalStrut(ROUND_PANEL_GAP));

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

  private MatchNode createStandalonePlacementMatchNode() {
    final Match match = new Match();
    final JButton buttonOne = MatchCardFactory.createWrestlerButton();
    final JButton buttonTwo = MatchCardFactory.createWrestlerButton();
    final JPanel matchCard = MatchCardFactory.createMatchCard();

    matchCard.add(buttonOne);
    matchCard.add(Box.createVerticalStrut(MATCH_BUTTON_GAP));
    matchCard.add(buttonTwo);

    final MatchNode node = new MatchNode(match, buttonOne, buttonTwo, matchCard, BracketSection.PLACEMENT);
    buttonOne.addActionListener(e -> advanceWinner(node, 1));
    buttonTwo.addActionListener(e -> advanceWinner(node, 2));
    bracketNodes.add(node);
    return node;
  }

  private List<MatchNode> createRoundSection(
    JPanel bracketPanel,
    String roundName,
    int matchCount,
    int roundDepth,
    boolean withResetButton,
    BracketSection bracketSection
  ) {
    final JPanel roundPanel = new JPanel();
    roundPanel.setLayout(new BoxLayout(roundPanel, BoxLayout.Y_AXIS));
    roundPanel.setBorder(BorderFactory.createEmptyBorder(ROUND_OUTER_PADDING, ROUND_INNER_PADDING, ROUND_OUTER_PADDING, ROUND_INNER_PADDING));
    roundPanel.setMinimumSize(new Dimension(ROUND_COLUMN_WIDTH, 0));
    roundPanel.setMaximumSize(new Dimension(ROUND_COLUMN_WIDTH, Integer.MAX_VALUE));

    addRoundHeader(roundPanel, roundName);
    if (withResetButton) {
      addResetButtonToFinalRound(roundPanel);
      roundPanel.add(Box.createVerticalStrut(ROUND_HEADER_BOTTOM_SPACING));
    }

    final int topOffset = BracketLayout.calculateTopOffset(roundDepth);
    final int betweenGap = BracketLayout.calculateBetweenGap(roundDepth);
    if (topOffset > 0) {
      roundPanel.add(Box.createVerticalStrut(topOffset));
    }

    final List<MatchNode> nodes = new ArrayList<>();
    for (int i = 0; i < matchCount; i++) {
      final Match match = new Match();

      final JButton buttonOne = MatchCardFactory.createWrestlerButton();
      final JButton buttonTwo = MatchCardFactory.createWrestlerButton();
      final JPanel matchCard = MatchCardFactory.createMatchCard();

      matchCard.add(buttonOne);
      matchCard.add(Box.createVerticalStrut(MATCH_BUTTON_GAP));
      matchCard.add(buttonTwo);

      roundPanel.add(matchCard);
      if (i < matchCount - 1) {
        roundPanel.add(Box.createVerticalStrut(betweenGap));
      }

      final MatchNode node = new MatchNode(match, buttonOne, buttonTwo, matchCard, bracketSection);
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

    if (source.isCompleted() && source.getWinningSlot() != winningSlot) {
      clearParticipantInTarget(source.getNextMatch(), source.getNextSlot());
      clearParticipantInTarget(source.getLoserNextMatch(), source.getLoserNextSlot());
      resetDecisionsFrom(source.getNextMatch());
      resetDecisionsFrom(source.getLoserNextMatch());
    }

    source.markWinner(winningSlot);
    refreshMatchNode(source);

    final Wrestler winner = source.selectedWinner();
    final Wrestler loser = source.selectedLoser();

    assignWrestlerToTarget(source.getNextMatch(), source.getNextSlot(), winner);
    assignWrestlerToTarget(source.getLoserNextMatch(), source.getLoserNextSlot(), loser);

    repaintBracketBoard();
  }

  private void resetDecisionsFrom(MatchNode node) {
    if (node == null) {
      return;
    }

    node.clearDecision();

    MatchNode winnerNext = node.getNextMatch();
    if (winnerNext != null) {
      clearParticipantInTarget(winnerNext, node.getNextSlot());
      resetDecisionsFrom(winnerNext);
      refreshMatchNode(winnerNext);
    }

    MatchNode loserNext = node.getLoserNextMatch();
    if (loserNext != null) {
      clearParticipantInTarget(loserNext, node.getLoserNextSlot());
      resetDecisionsFrom(loserNext);
      refreshMatchNode(loserNext);
    }

    refreshMatchNode(node);
  }

  private void assignWrestlerToTarget(MatchNode target, int slot, Wrestler wrestler) {
    if (target == null) {
      return;
    }
    if (slot == 1) {
      target.getMatch().setWrestlerOne(wrestler);
    } else {
      target.getMatch().setWrestlerTwo(wrestler);
    }
    refreshMatchNode(target);
  }

  private void clearParticipantInTarget(MatchNode target, int slot) {
    assignWrestlerToTarget(target, slot, null);
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

