package constants;

import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.nio.file.Path;

@UtilityClass
public class Constants {

  public static final String WINDOW_TITLE = "Wrestling Bracket";
  public static final Dimension WINDOW_SIZE = new Dimension(1850, 1000);
  public static final Dimension WRESTLER_BUTTON_SIZE = new Dimension(264, 24);
  public static final int MATCH_CARD_WIDTH = 278;
  public static final int MATCH_CARD_HEIGHT = 63;
  public static final int ROUND_COLUMN_WIDTH = 296;
  public static final int ROUND_OF_32_GAP = 16;
  public static final int SCROLL_UNIT_INCREMENT = 28;
  public static final int SCROLL_BLOCK_INCREMENT = 84;
  public static final int ROUND_OUTER_PADDING = 8;
  public static final int ROUND_INNER_PADDING = 6;
  public static final int ROUND_PANEL_GAP = 4;
  public static final int ROUND_HEADER_HEIGHT = 28;
  public static final int ROUND_HEADER_BOTTOM_SPACING = 12;
  public static final int MATCH_BUTTON_GAP = 3;
  public static final int CONNECTOR_MIN_HORIZONTAL_SEGMENT = 24;
  public static final float ROUND_HEADER_FONT_SIZE = 14f;
  public static final float BUTTON_FONT_SIZE = 11f;
  public static final Color MATCH_CARD_BORDER_COLOR = new Color(210, 210, 210);
  public static final Color CONNECTOR_COLOR = new Color(120, 120, 120);
  public static final Color WINNER_COLOR = new Color(198, 239, 206);
  public static final Color LOSER_COLOR = new Color(255, 199, 206);
  public static final Path DEFAULT_SEEDING_FILE = Path.of("src", "test", "java", "SeedingTest.xlsx");
  public static final List<Path> SEEDING_FILE_CANDIDATES = List.of(
    Path.of("src", "test", "java", "SeedingTest.xlsx"),
    Path.of("interactive-wrestling-brackets", "src", "test", "java", "SeedingTest.xlsx")
  );
  public static final String SHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

}
