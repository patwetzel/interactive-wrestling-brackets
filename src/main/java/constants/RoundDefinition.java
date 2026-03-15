package constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoundDefinition {
  PIGTAIL("Pigtail", 1, -1, BracketSection.CHAMPIONSHIP),
  ROUND_OF_32("Round of 32", 16, 0, BracketSection.CHAMPIONSHIP),
  ROUND_OF_16("Round of 16", 8, 1, BracketSection.CHAMPIONSHIP),
  QUARTERFINALS("Quarterfinals", 4, 2, BracketSection.CHAMPIONSHIP),
  SEMIFINALS("Semifinals", 2, 3, BracketSection.CHAMPIONSHIP),
  FINAL("Final", 1, 4, BracketSection.CHAMPIONSHIP),

  CONSOLATION_PIGTAIL("Consi Pigtail", 1, -1, BracketSection.CONSOLATION),
  CONSOLATION_ROUND_1("Consi Round 1", 8, 0, BracketSection.CONSOLATION),
  CONSOLATION_ROUND_2("Consi Round 2", 8, 0, BracketSection.CONSOLATION),
  CONSOLATION_ROUND_3("Consi Round 3", 4, 1, BracketSection.CONSOLATION),
  BLOOD_ROUND("Blood Round", 4, 1, BracketSection.CONSOLATION),
  BLOOD_ROUND_WINNERS("Consi Quarterfinals", 2, 2, BracketSection.CONSOLATION),
  CONSOLATION_SEMIFINALS("Consi Semifinals", 2, 2, BracketSection.CONSOLATION),
  THIRD_PLACE("3rd Place", 1, 3, BracketSection.CONSOLATION),
  FIFTH_PLACE("5th Place", 1, 3, BracketSection.PLACEMENT),
  SEVENTH_PLACE("7th Place", 1, 3, BracketSection.PLACEMENT);

  private final String displayName;
  private final int matchCount;
  private final int roundDepth;
  private final BracketSection bracketSection;

  public boolean isPlacementRound() {
    return bracketSection == BracketSection.PLACEMENT;
  }

  public static RoundDefinition[] championshipRounds() {
    return new RoundDefinition[] {
      PIGTAIL, ROUND_OF_32, ROUND_OF_16, QUARTERFINALS, SEMIFINALS, FINAL
    };
  }
}
