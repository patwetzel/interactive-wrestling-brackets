package bracket;

import lombok.RequiredArgsConstructor;
import constants.BracketSection;
import match.MatchNode;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

import static constants.Constants.CONNECTOR_MIN_HORIZONTAL_SEGMENT;
import static constants.Constants.CONNECTOR_MIN_FINAL_SEGMENT;
import static constants.Constants.CONNECTOR_COLOR;

@RequiredArgsConstructor
public final class BracketBoardPanel extends JPanel {
  private static final BasicStroke CONNECTOR_STROKE = new BasicStroke(2f);

  private final List<MatchNode> bracketNodes;

  @Override
  protected void paintChildren(Graphics g) {
    super.paintChildren(g);
    drawConnectors((Graphics2D) g.create());
  }

  private void drawConnectors(Graphics2D graphics) {
    graphics.setColor(CONNECTOR_COLOR);
    graphics.setStroke(CONNECTOR_STROKE);

    for (MatchNode source : bracketNodes) {
      drawConnection(
        graphics,
        source,
        source.getNextMatch(),
        source.getNextSlot(),
        source.getMatchCard(),
        source.getMatchCard().getWidth(),
        source.getMatchCard().getHeight() / 2
      );
    }

    graphics.dispose();
  }

  private void drawConnection(
    Graphics2D graphics,
    MatchNode source,
    MatchNode targetMatch,
    int targetSlot,
    java.awt.Component sourceComponent,
    int sourceX,
    int sourceY
  ) {
    if (targetMatch == null || !sourceComponent.isShowing() || !targetMatch.getMatchCard().isShowing()) {
      return;
    }
    if (!shouldDrawConnection(source.getBracketSection(), targetMatch.getBracketSection())) {
      return;
    }

    JButton targetButton = targetSlot == 1 ? targetMatch.getButtonOne() : targetMatch.getButtonTwo();

    Point sourcePoint = SwingUtilities.convertPoint(
      sourceComponent,
      sourceX,
      sourceY,
      this
    );
    Point targetPoint = SwingUtilities.convertPoint(
      targetButton,
      0,
      targetButton.getHeight() / 2,
      this
    );

    int midX = computeElbowX(sourcePoint.x, targetPoint.x);

    graphics.drawLine(sourcePoint.x, sourcePoint.y, midX, sourcePoint.y);
    graphics.drawLine(midX, sourcePoint.y, midX, targetPoint.y);
    graphics.drawLine(midX, targetPoint.y, targetPoint.x, targetPoint.y);
  }

  private boolean shouldDrawConnection(BracketSection sourceSection, BracketSection targetSection) {
    if (sourceSection == BracketSection.PLACEMENT || targetSection == BracketSection.PLACEMENT) {
      return false;
    }

    boolean crossesChampionshipAndConsolation =
      (sourceSection == BracketSection.CHAMPIONSHIP && targetSection == BracketSection.CONSOLATION)
        || (sourceSection == BracketSection.CONSOLATION && targetSection == BracketSection.CHAMPIONSHIP);
    if (crossesChampionshipAndConsolation) {
      return false;
    }

    return sourceSection == targetSection;
  }

  private int computeElbowX(int sourceX, int targetX) {
    int delta = targetX - sourceX;
    if (delta > 0) {
      int preferred = sourceX + Math.max(CONNECTOR_MIN_HORIZONTAL_SEGMENT, delta / 2);
      int maxElbow = targetX - CONNECTOR_MIN_FINAL_SEGMENT;
      return Math.max(sourceX + 1, Math.min(preferred, maxElbow));
    }
    if (delta < 0) {
      int preferred = sourceX - Math.max(CONNECTOR_MIN_HORIZONTAL_SEGMENT, (-delta) / 2);
      int minElbow = targetX + CONNECTOR_MIN_FINAL_SEGMENT;
      return Math.min(sourceX - 1, Math.max(preferred, minElbow));
    }
    return sourceX;
  }
}
