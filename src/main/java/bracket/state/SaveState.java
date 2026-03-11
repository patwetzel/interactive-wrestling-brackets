package bracket.state;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public final class SaveState implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  public final String selectedSheetName;
  public final Map<String, List<Integer>> weightProgressBySheet;
  public final Map<String, List<String>> allAmericanLabelsBySheet;

  public SaveState(
    String selectedSheetName,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet
  ) {
    this.selectedSheetName = selectedSheetName;
    this.weightProgressBySheet = weightProgressBySheet;
    this.allAmericanLabelsBySheet = allAmericanLabelsBySheet;
  }
}
