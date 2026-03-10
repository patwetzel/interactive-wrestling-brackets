package bracket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BracketLayoutTest {

  @Test
  void calculateTopOffsetMatchesExpectedSpacingPerRound() {
    assertEquals(0, BracketLayout.calculateTopOffset(-1));
    assertEquals(0, BracketLayout.calculateTopOffset(0));
    assertEquals(40, BracketLayout.calculateTopOffset(1));
    assertEquals(119, BracketLayout.calculateTopOffset(2));
  }

  @Test
  void calculateBetweenGapMatchesExpectedSpacingPerRound() {
    assertEquals(8, BracketLayout.calculateBetweenGap(-1));
    assertEquals(16, BracketLayout.calculateBetweenGap(0));
    assertEquals(95, BracketLayout.calculateBetweenGap(1));
    assertEquals(253, BracketLayout.calculateBetweenGap(2));
  }
}
