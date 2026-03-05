package match;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchNodeTest {

  @Test
  void markWinnerAndClearDecisionTrackStateTransitions() {
    MatchNode node = new MatchNode(new Match(), new JButton(), new JButton(), new JPanel());

    assertFalse(node.isCompleted());
    assertEquals(0, node.getWinningSlot());

    node.markWinner(2);

    assertTrue(node.isCompleted());
    assertEquals(2, node.getWinningSlot());
    assertNull(node.selectedWinner());

    node.clearDecision();

    assertFalse(node.isCompleted());
    assertEquals(0, node.getWinningSlot());
  }
}
