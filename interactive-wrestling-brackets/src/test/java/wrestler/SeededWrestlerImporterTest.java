package wrestler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

import static constants.Constants.DEFAULT_SEEDING_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeededWrestlerImporterTest {

  @Test
  void readsDefaultSeedingFileWithExpectedCountAndSeedBounds() throws Exception {
    assertTrue(Files.exists(DEFAULT_SEEDING_FILE));

    SeededWrestlerImporter importer = new SeededWrestlerImporter();
    List<Wrestler> wrestlers = importer.readSeededWrestlers(DEFAULT_SEEDING_FILE);

    assertEquals(33, wrestlers.size());

    wrestlers.sort(Comparator.comparingInt(Wrestler::getSeed));
    assertEquals(1, wrestlers.get(0).getSeed());
    assertEquals(33, wrestlers.get(wrestlers.size() - 1).getSeed());
  }

  @Test
  void readsWeightSheetNamesFromWorkbook() throws Exception {
    assertTrue(Files.exists(DEFAULT_SEEDING_FILE));

    SeededWrestlerImporter importer = new SeededWrestlerImporter();
    List<String> sheetNames = importer.readSheetNames(DEFAULT_SEEDING_FILE);

    assertEquals(10, sheetNames.size());
    assertEquals("125", sheetNames.get(0));
    assertEquals("285", sheetNames.get(sheetNames.size() - 1));
  }
}
