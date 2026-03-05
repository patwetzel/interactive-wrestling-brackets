package constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoundDefinition {
  PIGTAIL("Pigtail", 1, -1, false),
  ROUND_OF_32("Round of 32", 16, 0, false),
  ROUND_OF_16("Round of 16", 8, 1, false),
  QUARTERFINALS("Quarterfinals", 4, 2, false),
  SEMIFINALS("Semifinals", 2, 3, false),
  FINAL("Final", 1, 4, true);

  private final String displayName;
  private final int matchCount;
  private final int roundDepth;
  private final boolean resetButton;
}
