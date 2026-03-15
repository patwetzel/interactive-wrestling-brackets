package bracket;

import static constants.Constants.*;

import constants.BracketSection;
import constants.RoundDefinition;
import bracket.logic.BracketConnectorService;
import bracket.state.BracketStateStore;
import bracket.ui.AllAmericansPanelBuilder;
import bracket.ui.BracketLayoutBuilder;
import bracket.ui.TeamScoresPanelBuilder;
import bracket.ui.WeightTabsController;
import match.Match;
import match.MatchCardFactory;
import match.MatchNode;
import wrestler.SeededWrestlerImporter;
import wrestler.Wrestler;
import wrestler.WrestlerLabelFormatter;

import javax.swing.*;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Bracket {

  private static final int ALL_AMERICAN_COUNT = 8;
  private static final Color ALL_AMERICAN_FIRST_COLOR = new Color(255, 243, 196);
  private static final Color ALL_AMERICAN_SECOND_COLOR = new Color(236, 240, 243);
  private static final Color ALL_AMERICAN_THIRD_COLOR = new Color(242, 214, 191);
  private static final Color ALL_AMERICAN_OTHER_COLOR = new Color(220, 236, 255);
  private static final String ALL_AMERICANS_TAB_LABEL = "All-Americans";
  private static final String TEAM_SCORES_TAB_LABEL = "Team Scores";
  private static final int MAX_WEIGHT_TABS = 10;
  private static final double DRAG_SCROLL_SPEED = 1.35;
  private static final double CHAMPIONSHIP_WIN_POINTS = 1.0;
  private static final double CONSOLATION_WIN_POINTS = 0.5;

  private final JFrame frame = new JFrame(WINDOW_TITLE);
  private final JPanel buttonPanel = new JPanel();
  private final JPanel weightTabsPanel = new JPanel();
  private final JScrollPane bracketScrollPane = new JScrollPane(buttonPanel);
  private final List<MatchNode> bracketNodes = new ArrayList<>();
  private final List<Wrestler> initialSeededWrestlers = new ArrayList<>();
  private final SeededWrestlerImporter seededWrestlerImporter = new SeededWrestlerImporter();
  private final Map<String, List<Integer>> weightProgressBySheet = new HashMap<>();
  private final Map<String, List<String>> allAmericanLabelsBySheet = new HashMap<>();
  private final Map<String, List<JLabel>> allAmericanOverviewNodesBySheet = new HashMap<>();
  private final WeightTabsController weightTabsController = new WeightTabsController(weightTabsPanel);
  private final AllAmericansPanelBuilder allAmericansPanelBuilder = new AllAmericansPanelBuilder(
    ALL_AMERICAN_COUNT,
    ALL_AMERICAN_FIRST_COLOR,
    ALL_AMERICAN_SECOND_COLOR,
    ALL_AMERICAN_THIRD_COLOR,
    ALL_AMERICAN_OTHER_COLOR,
    ALL_AMERICANS_TAB_LABEL
  );
  private final TeamScoresPanelBuilder teamScoresPanelBuilder = new TeamScoresPanelBuilder(TEAM_SCORES_TAB_LABEL);
  private final BracketStateStore stateStore = new BracketStateStore(frame, BracketStateStore.DEFAULT_SAVE_STATE_PATH);
  private final BracketLayoutBuilder layoutBuilder = new BracketLayoutBuilder(
    this::createMatchNode,
    this::openStateFromDisk,
    this::saveStateToDisk,
    this::resetBracket,
    allAmericansPanelBuilder
  );

  private Path seedingFilePath;
  private String selectedSheetName;
  private String lastWeightSheetName;
  private boolean showingAllAmericans;
  private boolean showingTeamScores;

  private BracketBoardPanel bracketBoard;
  private MatchNode finalMatchNode;
  private MatchNode thirdPlaceMatchNode;
  private MatchNode fifthPlaceMatchNode;
  private MatchNode seventhPlaceMatchNode;
  private final List<JLabel> allAmericanNodes = new ArrayList<>();
  private AWTEventListener dragScrollListener;
  private Point dragStartScreen;
  private int dragStartHorizontal;
  private int dragStartVertical;
  private boolean suppressProgressPersistence;

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
    frame.setLayout(new BorderLayout());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.getContentPane().setBackground(APP_BACKGROUND_COLOR);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
  }

  private void setupPanels() {
    final JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setOpaque(true);
    mainPanel.setBackground(APP_BACKGROUND_COLOR);

    buttonPanel.setBorder(BorderFactory.createEmptyBorder());
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    buttonPanel.setOpaque(true);
    buttonPanel.setBackground(APP_BACKGROUND_COLOR);

    bracketScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
    bracketScrollPane.getVerticalScrollBar().setBlockIncrement(SCROLL_BLOCK_INCREMENT);
    bracketScrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
    bracketScrollPane.getHorizontalScrollBar().setBlockIncrement(SCROLL_BLOCK_INCREMENT);
    bracketScrollPane.setWheelScrollingEnabled(false);
    bracketScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    bracketScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    bracketScrollPane.setBorder(BorderFactory.createEmptyBorder());
    bracketScrollPane.getViewport().setBackground(APP_BACKGROUND_COLOR);

    enableDragScrolling();

    mainPanel.add(bracketScrollPane, BorderLayout.CENTER);
    weightTabsPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
    weightTabsPanel.setOpaque(true);
    weightTabsPanel.setBackground(APP_BACKGROUND_COLOR);
    mainPanel.add(weightTabsPanel, BorderLayout.SOUTH);
    frame.add(mainPanel, BorderLayout.CENTER);
  }

  private void enableDragScrolling() {
    if (dragScrollListener != null) {
      return;
    }
    dragScrollListener = event -> {
      if (!(event instanceof MouseEvent mouseEvent)) {
        return;
      }

      if (!SwingUtilities.isLeftMouseButton(mouseEvent)) {
        return;
      }

      if (!isEventInFrame(mouseEvent) || isEventOverScrollbar(mouseEvent) || isEventOverWeightTabs(mouseEvent)) {
        return;
      }

      switch (mouseEvent.getID()) {
        case MouseEvent.MOUSE_PRESSED:
          startDragScroll(mouseEvent);
          break;
        case MouseEvent.MOUSE_DRAGGED:
          updateDragScroll(mouseEvent);
          break;
        case MouseEvent.MOUSE_RELEASED:
          finishDragScroll();
          break;
        default:
          break;
      }
    };

    Toolkit.getDefaultToolkit().addAWTEventListener(
      dragScrollListener,
      AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
    );
  }

  private boolean isEventInFrame(MouseEvent event) {
    Object source = event.getSource();
    if (!(source instanceof Component component)) {
      return false;
    }
    return SwingUtilities.isDescendingFrom(component, frame);
  }

  private boolean isEventOverScrollbar(MouseEvent event) {
    Object source = event.getSource();
    if (!(source instanceof Component component)) {
      return false;
    }
    return SwingUtilities.getAncestorOfClass(JScrollBar.class, component) != null;
  }

  private boolean isEventOverWeightTabs(MouseEvent event) {
    Object source = event.getSource();
    if (!(source instanceof Component component)) {
      return false;
    }
    return SwingUtilities.isDescendingFrom(component, weightTabsPanel);
  }

  private void startDragScroll(MouseEvent event) {
    dragStartScreen = event.getLocationOnScreen();
    dragStartHorizontal = bracketScrollPane.getHorizontalScrollBar().getValue();
    dragStartVertical = bracketScrollPane.getVerticalScrollBar().getValue();
  }

  private void updateDragScroll(MouseEvent event) {
    if (dragStartScreen == null) {
      return;
    }

    Point current = event.getLocationOnScreen();
    int deltaX = current.x - dragStartScreen.x;
    int deltaY = current.y - dragStartScreen.y;

    int scaledX = (int) Math.round(deltaX * DRAG_SCROLL_SPEED);
    int scaledY = (int) Math.round(deltaY * DRAG_SCROLL_SPEED);
    setScrollBarValue(bracketScrollPane.getHorizontalScrollBar(), dragStartHorizontal - scaledX);
    setScrollBarValue(bracketScrollPane.getVerticalScrollBar(), dragStartVertical - scaledY);
  }

  private void finishDragScroll() {
    if (dragStartScreen == null) {
      return;
    }

    dragStartScreen = null;
  }

  private void setScrollBarValue(JScrollBar scrollBar, int value) {
    int min = scrollBar.getMinimum();
    int max = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
    int clamped = Math.max(min, Math.min(max, value));
    scrollBar.setValue(clamped);
  }

  private void loadInitialSeedingIfPresent() {
    final Optional<Path> seedingPathOpt = stateStore.resolveSeedingPath(SEEDING_FILE_CANDIDATES);
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
      if (!loadSavedStateIfPresent(sheetNames)) {
        loadWeightSheet(sheetNames.get(0));
      }
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
    weightTabsController.buildTabs(
      sheetNames,
      MAX_WEIGHT_TABS,
      ALL_AMERICANS_TAB_LABEL,
      TEAM_SCORES_TAB_LABEL,
      this::loadWeightSheet,
      this::showAllAmericansPage,
      this::showTeamScoresPage
    );
  }

  private void loadWeightSheet(String sheetName) {
    loadWeightSheet(sheetName, false);
  }

  private void loadWeightSheetForRestore(String sheetName) {
    loadWeightSheet(sheetName, true);
  }

  private void loadWeightSheet(String sheetName, boolean skipSaveCurrent) {
    if (seedingFilePath == null) {
      return;
    }

    showingAllAmericans = false;
    showingTeamScores = false;
    if (!skipSaveCurrent && selectedSheetName != null && !selectedSheetName.equals(sheetName)) {
      saveCurrentWeightProgress(selectedSheetName);
    }

    try {
      final List<Wrestler> seededWrestlers = seededWrestlerImporter.readSeededWrestlers(seedingFilePath, sheetName);
      if (seededWrestlers.size() != EXPECTED_WRESTLER_COUNT) {
        JOptionPane.showMessageDialog(
          frame,
          "Expected " + EXPECTED_WRESTLER_COUNT + " wrestlers in sheet '" + sheetName + "' but found " + seededWrestlers.size() + ".",
          "Seeding File Error",
          JOptionPane.WARNING_MESSAGE
        );
        return;
      }

      initializeSeededWrestlers(seededWrestlers);
      selectedSheetName = sheetName;
      lastWeightSheetName = sheetName;
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
    weightTabsController.updateState(selectedSheetName, showingAllAmericans, showingTeamScores);
  }

  private void initializeSeededWrestlers(List<Wrestler> seededWrestlers) {
    seededWrestlers.sort(Comparator.comparingInt(Wrestler::getSeed));
    initialSeededWrestlers.clear();
    initialSeededWrestlers.addAll(seededWrestlers);
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

    suppressProgressPersistence = true;
    try {
      int limit = Math.min(winningSlots.size(), bracketNodes.size());
      boolean progressed;
      do {
        progressed = false;
        for (int i = 0; i < limit; i++) {
          int winningSlot = winningSlots.get(i);
          if (winningSlot != 1 && winningSlot != 2) {
            continue;
          }

          MatchNode node = bracketNodes.get(i);
          if (node.isCompleted()) {
            continue;
          }

          if (node.getMatch().getWrestlerOne() == null || node.getMatch().getWrestlerTwo() == null) {
            continue;
          }

          advanceWinner(node, winningSlot);
          progressed = true;
        }
      } while (progressed);
    } finally {
      suppressProgressPersistence = false;
    }

    saveCurrentWeightProgress(sheetName);
  }

  private void renderSeededBracket(List<Wrestler> seededWrestlers) {
    final Map<Integer, Wrestler> bySeed = mapBySeed(seededWrestlers);

    bracketNodes.clear();
    resetBracketPanel();
    final BracketLayoutBuilder.LayoutResult layout = layoutBuilder.buildLayout(bracketNodes, allAmericanNodes);
    bracketBoard = layout.board();

    final EnumMap<RoundDefinition, List<MatchNode>> rounds = layout.rounds();
    final EnumMap<RoundDefinition, List<MatchNode>> consolationRounds = layout.consolationRounds();
    final EnumMap<RoundDefinition, MatchNode> placementMatches = layout.placementMatches();
    bindPlacementRankingNodes(rounds, consolationRounds, placementMatches);

    BracketConnectorService.seedOpeningMatches(bySeed, rounds.get(RoundDefinition.PIGTAIL), rounds.get(RoundDefinition.ROUND_OF_32));
    BracketConnectorService.connectRounds(rounds, consolationRounds, placementMatches);

    buttonPanel.add(bracketBoard, BorderLayout.NORTH);

    refreshAllMatchNodes();
    buttonPanel.revalidate();
    buttonPanel.repaint();
  }

  private void resetBracketPanel() {
    buttonPanel.removeAll();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder());
    buttonPanel.setLayout(new BorderLayout());
  }

  private void refreshAllMatchNodes() {
    for (MatchNode node : bracketNodes) {
      refreshMatchNode(node);
    }
  }

  private Map<Integer, Wrestler> mapBySeed(List<Wrestler> seededWrestlers) {
    final Map<Integer, Wrestler> bySeed = new HashMap<>();
    for (Wrestler seeded : seededWrestlers) {
      bySeed.put(seeded.getSeed(), seeded);
    }
    return bySeed;
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

    refreshAllAmericanNodes();
    refreshTeamScoresPanel();
    repaintBracketBoard();
    persistCurrentWeightProgress();
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

  private MatchNode createMatchNode(BracketSection section) {
    final JButton buttonOne = MatchCardFactory.createWrestlerButton();
    final JButton buttonTwo = MatchCardFactory.createWrestlerButton();
    final JPanel matchCard = MatchCardFactory.createMatchCard();
    matchCard.add(buttonOne);
    matchCard.add(Box.createVerticalStrut(MATCH_BUTTON_GAP));
    matchCard.add(buttonTwo);

    final MatchNode node = new MatchNode(new Match(), buttonOne, buttonTwo, matchCard, section);
    buttonOne.addActionListener(e -> advanceWinner(node, 1));
    buttonTwo.addActionListener(e -> advanceWinner(node, 2));
    return node;
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

  private void bindPlacementRankingNodes(
    EnumMap<RoundDefinition, List<MatchNode>> rounds,
    EnumMap<RoundDefinition, List<MatchNode>> consolationRounds,
    EnumMap<RoundDefinition, MatchNode> placementMatches
  ) {
    finalMatchNode = rounds.get(RoundDefinition.FINAL).get(0);
    thirdPlaceMatchNode = consolationRounds.get(RoundDefinition.THIRD_PLACE).get(0);
    fifthPlaceMatchNode = placementMatches.get(RoundDefinition.FIFTH_PLACE);
    seventhPlaceMatchNode = placementMatches.get(RoundDefinition.SEVENTH_PLACE);
    refreshAllAmericanNodes();
  }

  private void refreshAllAmericanNodes() {
    if (allAmericanNodes.isEmpty()) {
      return;
    }

    final Wrestler[] placements = new Wrestler[] {
      selectedWinner(finalMatchNode),
      selectedLoser(finalMatchNode),
      selectedWinner(thirdPlaceMatchNode),
      selectedLoser(thirdPlaceMatchNode),
      selectedWinner(fifthPlaceMatchNode),
      selectedLoser(fifthPlaceMatchNode),
      selectedWinner(seventhPlaceMatchNode),
      selectedLoser(seventhPlaceMatchNode)
    };

    for (int i = 0; i < allAmericanNodes.size() && i < placements.length; i++) {
      final JLabel node = allAmericanNodes.get(i);
      node.setText(formatAllAmericanLabel(i + 1, placements[i]));
    }

    storeAllAmericansForCurrentSheet(placements);
  }

  private String formatAllAmericanLabel(int placement, Wrestler wrestler) {
    final String prefix = "#" + placement + " ";
    if (wrestler == null) {
      return prefix + "TBD";
    }
    final String seedSuffix = wrestler.getSeed() > 0 ? " (" + wrestler.getSeed() + ")" : "";
    return prefix + wrestler.getName() + " (" + wrestler.getTeam() + ") "
      + wrestler.getWins() + "-" + wrestler.getLosses() + seedSuffix;
  }

  private Wrestler selectedWinner(MatchNode node) {
    return node == null ? null : node.selectedWinner();
  }

  private Wrestler selectedLoser(MatchNode node) {
    return node == null ? null : node.selectedLoser();
  }

  private void repaintBracketBoard() {
    if (bracketBoard != null) {
      bracketBoard.repaint();
    }
  }

  private void showAllAmericansPage() {
    if (seedingFilePath == null) {
      return;
    }
    if (selectedSheetName != null) {
      saveCurrentWeightProgress(selectedSheetName);
    }
    selectedSheetName = null;
    showingAllAmericans = true;
    showingTeamScores = false;
    updateWeightTabState();
    renderAllAmericansOverview();
  }

  private void showTeamScoresPage() {
    if (seedingFilePath == null) {
      return;
    }
    if (selectedSheetName != null) {
      saveCurrentWeightProgress(selectedSheetName);
    }
    selectedSheetName = null;
    showingAllAmericans = false;
    showingTeamScores = true;
    updateWeightTabState();
    renderTeamScoresPanel();
  }

  private void storeAllAmericansForCurrentSheet(Wrestler[] placements) {
    if (selectedSheetName == null) {
      return;
    }

    final List<String> snapshot = new ArrayList<>();
    for (int i = 0; i < placements.length; i++) {
      snapshot.add(formatAllAmericanLabel(i + 1, placements[i]));
    }
    allAmericanLabelsBySheet.put(selectedSheetName, snapshot);
  }

  private void renderAllAmericansOverview() {
    resetBracketPanel();

    final AllAmericansPanelBuilder.OverviewResult result = allAmericansPanelBuilder.buildOverviewPanel(
      weightTabsController.getWeightSheetNames()
    );
    allAmericanOverviewNodesBySheet.clear();
    allAmericanOverviewNodesBySheet.putAll(result.nodesBySheet());
    buttonPanel.add(result.panel(), BorderLayout.NORTH);
    refreshAllAmericansOverview();
    buttonPanel.revalidate();
    buttonPanel.repaint();
  }

  private void renderTeamScoresPanel() {
    resetBracketPanel();

    final Map<String, Double> teamScores = calculateTeamScores();
    final JPanel panel = teamScoresPanelBuilder.buildTeamScoresPanel(teamScores);
    buttonPanel.add(panel, BorderLayout.NORTH);
    buttonPanel.revalidate();
    buttonPanel.repaint();
  }

  private void refreshTeamScoresPanel() {
    if (!showingTeamScores) {
      return;
    }
    renderTeamScoresPanel();
  }

  private Map<String, Double> calculateTeamScores() {
    return calculateTeamScoresAcrossWeights(weightTabsController.getWeightSheetNames());
  }

  private Map<String, Double> calculateTeamScoresAcrossWeights(List<String> sheetNames) {
    final Map<String, Double> teamScores = new HashMap<>();
    if (seedingFilePath == null || sheetNames == null || sheetNames.isEmpty()) {
      return teamScores;
    }

    for (String sheetName : sheetNames) {
      try {
        final List<Wrestler> seededWrestlers = seededWrestlerImporter.readSeededWrestlers(seedingFilePath, sheetName);
        if (seededWrestlers.size() != EXPECTED_WRESTLER_COUNT) {
          continue;
        }

        final ScoreBracketState state = buildScoreBracketState(seededWrestlers);
        applyWinningSlots(state.bracketNodes, weightProgressBySheet.get(sheetName));
        addMatchWinPoints(teamScores, state.bracketNodes);
        addPlacementPoints(teamScores, state);
      } catch (Exception ignored) {
        // Skip sheets that fail to load while aggregating.
      }
    }

    return teamScores;
  }

  private ScoreBracketState buildScoreBracketState(List<Wrestler> seededWrestlers) {
    final Map<Integer, Wrestler> bySeed = mapBySeed(seededWrestlers);
    final List<MatchNode> scoringNodes = new ArrayList<>();
    final List<JLabel> scoringAllAmericanNodes = new ArrayList<>();
    final BracketLayoutBuilder.LayoutResult layout = layoutBuilder.buildLayout(scoringNodes, scoringAllAmericanNodes);

    final EnumMap<RoundDefinition, List<MatchNode>> rounds = layout.rounds();
    final EnumMap<RoundDefinition, List<MatchNode>> consolationRounds = layout.consolationRounds();
    final EnumMap<RoundDefinition, MatchNode> placementMatches = layout.placementMatches();

    BracketConnectorService.seedOpeningMatches(bySeed, rounds.get(RoundDefinition.PIGTAIL), rounds.get(RoundDefinition.ROUND_OF_32));
    BracketConnectorService.connectRounds(rounds, consolationRounds, placementMatches);

    return new ScoreBracketState(
      scoringNodes,
      rounds.get(RoundDefinition.FINAL).get(0),
      consolationRounds.get(RoundDefinition.THIRD_PLACE).get(0),
      placementMatches.get(RoundDefinition.FIFTH_PLACE),
      placementMatches.get(RoundDefinition.SEVENTH_PLACE)
    );
  }

  private void applyWinningSlots(List<MatchNode> nodes, List<Integer> winningSlots) {
    if (nodes == null || nodes.isEmpty() || winningSlots == null || winningSlots.isEmpty()) {
      return;
    }

    int limit = Math.min(winningSlots.size(), nodes.size());
    boolean progressed;
    do {
      progressed = false;
      for (int i = 0; i < limit; i++) {
        int winningSlot = winningSlots.get(i);
        if (winningSlot != 1 && winningSlot != 2) {
          continue;
        }

        MatchNode node = nodes.get(i);
        if (node.isCompleted()) {
          continue;
        }

        if (node.getMatch().getWrestlerOne() == null || node.getMatch().getWrestlerTwo() == null) {
          continue;
        }

        advanceWinnerSilent(node, winningSlot);
        progressed = true;
      }
    } while (progressed);
  }

  private void advanceWinnerSilent(MatchNode source, int winningSlot) {
    source.markWinner(winningSlot);

    final Wrestler winner = source.selectedWinner();
    final Wrestler loser = source.selectedLoser();

    assignWrestlerToTarget(source.getNextMatch(), source.getNextSlot(), winner);
    assignWrestlerToTarget(source.getLoserNextMatch(), source.getLoserNextSlot(), loser);
  }

  private void addMatchWinPoints(Map<String, Double> teamScores, List<MatchNode> nodes) {
    for (MatchNode node : nodes) {
      if (!node.isCompleted()) {
        continue;
      }

      final Wrestler winner = node.selectedWinner();
      if (winner == null) {
        continue;
      }

      final double winPoints = winPointsForSection(node.getBracketSection());
      if (winPoints <= 0) {
        continue;
      }

      addTeamPoints(teamScores, winner, winPoints);
    }
  }

  private double winPointsForSection(BracketSection section) {
    return switch (section) {
      case CHAMPIONSHIP -> CHAMPIONSHIP_WIN_POINTS;
      case CONSOLATION, PLACEMENT -> CONSOLATION_WIN_POINTS;
      default -> 0.0;
    };
  }

  private void addPlacementPoints(Map<String, Double> teamScores, ScoreBracketState state) {
    addPlacementPointsForMatch(teamScores, state.finalMatchNode, 16, 12);
    addPlacementPointsForMatch(teamScores, state.thirdPlaceMatchNode, 10, 9);
    addPlacementPointsForMatch(teamScores, state.fifthPlaceMatchNode, 7, 6);
    addPlacementPointsForMatch(teamScores, state.seventhPlaceMatchNode, 4, 3);
  }

  private void addPlacementPointsForMatch(
    Map<String, Double> teamScores,
    MatchNode matchNode,
    double winnerPoints,
    double loserPoints
  ) {
    if (matchNode == null || !matchNode.isCompleted()) {
      return;
    }
    final Wrestler winner = matchNode.selectedWinner();
    final Wrestler loser = matchNode.selectedLoser();
    addTeamPoints(teamScores, winner, winnerPoints);
    addTeamPoints(teamScores, loser, loserPoints);
  }

  private void addTeamPoints(Map<String, Double> teamScores, Wrestler wrestler, double points) {
    if (wrestler == null) {
      return;
    }
    final String team = wrestler.getTeam() == null ? "" : wrestler.getTeam().toString();
    if (team.isBlank()) {
      return;
    }
    teamScores.merge(team, points, Double::sum);
  }

  private static final class ScoreBracketState {
    private final List<MatchNode> bracketNodes;
    private final MatchNode finalMatchNode;
    private final MatchNode thirdPlaceMatchNode;
    private final MatchNode fifthPlaceMatchNode;
    private final MatchNode seventhPlaceMatchNode;

    private ScoreBracketState(
      List<MatchNode> bracketNodes,
      MatchNode finalMatchNode,
      MatchNode thirdPlaceMatchNode,
      MatchNode fifthPlaceMatchNode,
      MatchNode seventhPlaceMatchNode
    ) {
      this.bracketNodes = bracketNodes;
      this.finalMatchNode = finalMatchNode;
      this.thirdPlaceMatchNode = thirdPlaceMatchNode;
      this.fifthPlaceMatchNode = fifthPlaceMatchNode;
      this.seventhPlaceMatchNode = seventhPlaceMatchNode;
    }
  }

  private void refreshAllAmericansOverview() {
    allAmericansPanelBuilder.refreshOverviewNodes(
      allAmericanOverviewNodesBySheet,
      allAmericanLabelsBySheet,
      placement -> formatAllAmericanLabel(placement, null)
    );
  }

  private boolean loadSavedStateIfPresent(List<String> sheetNames) {
    return stateStore.loadSavedStateIfPresent(
      BracketStateStore.DEFAULT_SAVE_STATE_PATH,
      sheetNames,
      weightProgressBySheet,
      allAmericanLabelsBySheet,
      this::loadWeightSheetForRestore
    );
  }

  private void saveStateToDisk() {
    if (seedingFilePath == null) {
      return;
    }
    final String saveSheetName = selectedSheetName != null ? selectedSheetName : lastWeightSheetName;
    stateStore.saveStateToDisk(
      saveSheetName,
      weightProgressBySheet,
      allAmericanLabelsBySheet,
      () -> {
        final String currentSheet = selectedSheetName != null ? selectedSheetName : lastWeightSheetName;
        if (currentSheet != null) {
          saveCurrentWeightProgress(currentSheet);
        }
      }
    );
  }

  private void openStateFromDisk() {
    stateStore.openStateFromDisk(
      weightTabsController.getWeightSheetNames(),
      weightProgressBySheet,
      allAmericanLabelsBySheet,
      this::loadWeightSheetForRestore
    );
  }

  private void resetBracket() {
    if (!initialSeededWrestlers.isEmpty()) {
      if (selectedSheetName != null) {
        weightProgressBySheet.remove(selectedSheetName);
      }
      allAmericanLabelsBySheet.remove(selectedSheetName);
      renderSeededBracket(initialSeededWrestlers);
      persistCurrentWeightProgress();
    }
  }

  private void persistCurrentWeightProgress() {
    if (suppressProgressPersistence) {
      return;
    }
    if (selectedSheetName != null) {
      saveCurrentWeightProgress(selectedSheetName);
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(Bracket::new);
  }
}

