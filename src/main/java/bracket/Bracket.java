package bracket;

import static constants.Constants.*;

import constants.BracketSection;
import constants.RoundDefinition;
import bracket.logic.BracketConnectorService;
import bracket.state.BracketStateStore;
import bracket.ui.AllAmericansPanelBuilder;
import bracket.ui.BracketLayoutBuilder;
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
  private static final int MAX_WEIGHT_TABS = 10;
  private static final double DRAG_SCROLL_SPEED = 1.35;

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
      this::loadWeightSheet,
      this::showAllAmericansPage
    );
  }

  private void loadWeightSheet(String sheetName) {
    if (seedingFilePath == null) {
      return;
    }

    showingAllAmericans = false;
    if (selectedSheetName != null && !selectedSheetName.equals(sheetName)) {
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
    weightTabsController.updateState(selectedSheetName, showingAllAmericans);
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
    updateWeightTabState();
    renderAllAmericansOverview();
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
      this::loadWeightSheet
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
      this::loadWeightSheet
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

