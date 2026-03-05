package bracket;

import lombok.RequiredArgsConstructor;
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
import static constants.Constants.CONNECTOR_COLOR;

@RequiredArgsConstructor
public final class BracketBoardPanel extends JPanel {
  private final List<MatchNode> bracketNodes;

  @Override
  protected void paintChildren(Graphics g) {
    super.paintChildren(g);
    drawConnectors((Graphics2D) g.create());
  }

  private void drawConnectors(Graphics2D graphics) {
    graphics.setColor(CONNECTOR_COLOR);
    graphics.setStroke(new BasicStroke(2f));

    for (MatchNode source : bracketNodes) {
      MatchNode nextMatch = source.getNextMatch();
      if (nextMatch == null || !source.getMatchCard().isShowing() || !nextMatch.getMatchCard().isShowing()) {
        continue;
      }

      JButton targetButton = source.getNextSlot() == 1 ? nextMatch.getButtonOne() : nextMatch.getButtonTwo();

      Point sourcePoint = SwingUtilities.convertPoint(
        source.getMatchCard(),
        source.getMatchCard().getWidth(),
        source.getMatchCard().getHeight() / 2,
        this
      );
      Point targetPoint = SwingUtilities.convertPoint(
        targetButton,
        0,
        targetButton.getHeight() / 2,
        this
      );

      int midX = sourcePoint.x + Math.max(CONNECTOR_MIN_HORIZONTAL_SEGMENT, (targetPoint.x - sourcePoint.x) / 2);

      graphics.drawLine(sourcePoint.x, sourcePoint.y, midX, sourcePoint.y);
      graphics.drawLine(midX, sourcePoint.y, midX, targetPoint.y);
      graphics.drawLine(midX, targetPoint.y, targetPoint.x, targetPoint.y);
    }

    graphics.dispose();
  }
}
