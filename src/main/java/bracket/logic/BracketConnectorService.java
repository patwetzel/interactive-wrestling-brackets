package bracket.logic;

import bracket.BracketConnector;
import constants.RoundDefinition;
import match.Match;
import match.MatchNode;
import wrestler.Wrestler;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BracketConnectorService {

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

  private BracketConnectorService() {}

  public static void seedOpeningMatches(Map<Integer, Wrestler> bySeed, List<MatchNode> pigtail, List<MatchNode> roundOf32) {
    final Match pigtailMatch = pigtail.get(0).getMatch();
    pigtailMatch.setWrestlerOne(bySeed.get(33));
    pigtailMatch.setWrestlerTwo(bySeed.get(32));

    final Match firstRoundTop = roundOf32.get(0).getMatch();
    firstRoundTop.setWrestlerOne(bySeed.get(1));

    for (int i = 1; i < roundOf32.size(); i++) {
      final int firstSeed = ROUND_OF_32_SEED_ORDER[i - 1][0];
      final int secondSeed = ROUND_OF_32_SEED_ORDER[i - 1][1];
      final Match roundMatch = roundOf32.get(i).getMatch();
      roundMatch.setWrestlerOne(bySeed.get(firstSeed));
      roundMatch.setWrestlerTwo(bySeed.get(secondSeed));
    }
  }

  public static void connectRounds(
    EnumMap<RoundDefinition, List<MatchNode>> rounds,
    EnumMap<RoundDefinition, List<MatchNode>> consolationRounds,
    EnumMap<RoundDefinition, MatchNode> placementMatches
  ) {
    BracketConnector.connectWinner(rounds.get(RoundDefinition.PIGTAIL).get(0), rounds.get(RoundDefinition.ROUND_OF_32).get(0), 2);
    BracketConnector.connectRounds(rounds.get(RoundDefinition.ROUND_OF_32), rounds.get(RoundDefinition.ROUND_OF_16));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.ROUND_OF_16), rounds.get(RoundDefinition.QUARTERFINALS));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.QUARTERFINALS), rounds.get(RoundDefinition.SEMIFINALS));
    BracketConnector.connectRounds(rounds.get(RoundDefinition.SEMIFINALS), rounds.get(RoundDefinition.FINAL));
    connectConsolationPath(rounds, consolationRounds, placementMatches);
  }

  private static void connectConsolationPath(
    EnumMap<RoundDefinition, List<MatchNode>> rounds,
    EnumMap<RoundDefinition, List<MatchNode>> consolationRounds,
    EnumMap<RoundDefinition, MatchNode> placementMatches
  ) {
    final List<MatchNode> roundOf32 = rounds.get(RoundDefinition.ROUND_OF_32);
    final List<MatchNode> roundOf16 = rounds.get(RoundDefinition.ROUND_OF_16);
    final List<MatchNode> quarterfinals = rounds.get(RoundDefinition.QUARTERFINALS);
    final List<MatchNode> semifinals = rounds.get(RoundDefinition.SEMIFINALS);

    final MatchNode consiPigtail = consolationRounds.get(RoundDefinition.CONSOLATION_PIGTAIL).get(0);
    final List<MatchNode> consiRound1 = consolationRounds.get(RoundDefinition.CONSOLATION_ROUND_1);
    final List<MatchNode> consiRound2 = consolationRounds.get(RoundDefinition.CONSOLATION_ROUND_2);
    final List<MatchNode> consiRound3 = consolationRounds.get(RoundDefinition.CONSOLATION_ROUND_3);
    final List<MatchNode> bloodRound = consolationRounds.get(RoundDefinition.BLOOD_ROUND);
    final List<MatchNode> consiQuarterfinals = consolationRounds.get(RoundDefinition.BLOOD_ROUND_WINNERS);
    final List<MatchNode> consiSemifinals = consolationRounds.get(RoundDefinition.CONSOLATION_SEMIFINALS);
    final MatchNode thirdPlace = consolationRounds.get(RoundDefinition.THIRD_PLACE).get(0);
    final MatchNode fifthPlace = placementMatches.get(RoundDefinition.FIFTH_PLACE);
    final MatchNode seventhPlace = placementMatches.get(RoundDefinition.SEVENTH_PLACE);

    BracketConnector.connectLoser(rounds.get(RoundDefinition.PIGTAIL).get(0), consiPigtail, 1);
    BracketConnector.connectLoser(roundOf32.get(THREE_VS_THIRTY_ROUND_OF_32_INDEX), consiPigtail, 2);
    BracketConnector.connectWinner(consiPigtail, consiRound1.get(4), 1);

    for (int i = 0; i < roundOf32.size(); i++) {
      if (i == THREE_VS_THIRTY_ROUND_OF_32_INDEX) {
        continue;
      }
      BracketConnector.connectLoser(roundOf32.get(i), consiRound1.get(i / 2), (i % 2) + 1);
    }

    for (int i = 0; i < consiRound1.size(); i++) {
      BracketConnector.connectWinner(consiRound1.get(i), consiRound2.get(i), 1);
    }
    for (int i = 0; i < roundOf16.size(); i++) {
      final int invertedIndex = consiRound2.size() - 1 - i;
      BracketConnector.connectLoser(roundOf16.get(i), consiRound2.get(invertedIndex), 2);
    }

    BracketConnector.connectRounds(consiRound2, consiRound3);

    for (int i = 0; i < consiRound3.size(); i++) {
      BracketConnector.connectWinner(consiRound3.get(i), bloodRound.get(i), 1);
    }
    final int[] quarterToBloodIndex = {1, 0, 3, 2};
    for (int i = 0; i < quarterfinals.size(); i++) {
      final int targetIndex = i < quarterToBloodIndex.length ? quarterToBloodIndex[i] : i;
      BracketConnector.connectLoser(quarterfinals.get(i), bloodRound.get(targetIndex), 2);
    }

    BracketConnector.connectRounds(bloodRound, consiQuarterfinals);

    for (int i = 0; i < consiQuarterfinals.size(); i++) {
      BracketConnector.connectWinner(consiQuarterfinals.get(i), consiSemifinals.get(i), 2);
      BracketConnector.connectLoser(consiQuarterfinals.get(i), seventhPlace, i + 1);
    }

    for (int i = 0; i < semifinals.size(); i++) {
      final int invertedIndex = consiSemifinals.size() - 1 - i;
      BracketConnector.connectLoser(semifinals.get(i), consiSemifinals.get(invertedIndex), 1);
    }

    for (int i = 0; i < consiSemifinals.size(); i++) {
      BracketConnector.connectWinner(consiSemifinals.get(i), thirdPlace, (i % 2) + 1);
      BracketConnector.connectLoser(consiSemifinals.get(i), fifthPlace, (i % 2) + 1);
    }
  }
}
