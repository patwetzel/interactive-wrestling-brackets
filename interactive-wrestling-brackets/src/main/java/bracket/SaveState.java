package bracket;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

final class SaveState implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  final String selectedSheetName;
  final Map<String, List<Integer>> weightProgressBySheet;
  final Map<String, List<String>> allAmericanLabelsBySheet;

  SaveState(
    String selectedSheetName,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet
  ) {
    this.selectedSheetName = selectedSheetName;
    this.weightProgressBySheet = weightProgressBySheet;
    this.allAmericanLabelsBySheet = allAmericanLabelsBySheet;
  }
}
