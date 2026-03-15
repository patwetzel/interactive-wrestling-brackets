package bracket.state;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class BracketStateStore {

  public static final Path DEFAULT_SAVE_STATE_PATH = Path.of(
    System.getProperty("user.home"),
    ".interactive-wrestling-brackets",
    "state.bin"
  );

  private static final Set<String> LEGACY_CLASS_NAMES = Set.of(
    "bracket.Bracket$SaveState",
    "bracket.SaveState"
  );

  private final JFrame frame;
  private final Path defaultSavePath;
  private Path lastSavePath;

  public BracketStateStore(JFrame frame, Path defaultSavePath) {
    this.frame = frame;
    this.defaultSavePath = defaultSavePath;
    this.lastSavePath = defaultSavePath;
  }

  public Optional<Path> resolveSeedingPath(List<Path> candidates) {
    for (Path candidate : candidates) {
      final Path absolute = candidate.toAbsolutePath();
      if (Files.exists(absolute)) {
        return Optional.of(absolute);
      }
    }
    return Optional.empty();
  }

  public boolean loadSavedStateIfPresent(
    Path saveStatePath,
    List<String> sheetNames,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet,
    Consumer<String> loadWeightSheet
  ) {
    if (!Files.exists(saveStatePath)) {
      return false;
    }

    try {
      return loadStateFromPath(saveStatePath, sheetNames, weightProgressBySheet, allAmericanLabelsBySheet, loadWeightSheet, false);
    } catch (Exception e) {
      return false;
    }
  }

  public void saveStateToDisk(
    String selectedSheetName,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet,
    Runnable preSave
  ) {
    if (preSave != null) {
      preSave.run();
    }

    final Optional<Path> targetPath = chooseStateFile("Save Bracket State", true);
    if (targetPath.isEmpty()) {
      return;
    }

    lastSavePath = targetPath.get();
    saveStateToPath(targetPath.get(), selectedSheetName, weightProgressBySheet, allAmericanLabelsBySheet);
  }

  public void openStateFromDisk(
    List<String> sheetNames,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet,
    Consumer<String> loadWeightSheet
  ) {
    final Optional<Path> targetPath = chooseStateFile("Open Bracket State", false);
    if (targetPath.isEmpty()) {
      return;
    }
    lastSavePath = targetPath.get();

    try {
      loadStateFromPath(targetPath.get(), sheetNames, weightProgressBySheet, allAmericanLabelsBySheet, loadWeightSheet, true);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(
        frame,
        "Failed to load saved state: " + e.getMessage(),
        "Saved State Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }

  private Optional<Path> chooseStateFile(String title, boolean isSaveDialog) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(title);

    final Path initialDir = lastSavePath != null ? lastSavePath.getParent() : defaultSavePath.getParent();
    if (initialDir != null) {
      chooser.setCurrentDirectory(initialDir.toFile());
    }
    chooser.setFileFilter(new FileNameExtensionFilter("Bracket State (*.bin)", "bin"));

    if (isSaveDialog) {
      if (lastSavePath != null) {
        chooser.setSelectedFile(lastSavePath.toFile());
      }
      final int result = chooser.showSaveDialog(frame);
      if (result != JFileChooser.APPROVE_OPTION) {
        return Optional.empty();
      }
      return normalizeSavePath(chooser.getSelectedFile().toPath());
    }

    final int result = chooser.showOpenDialog(frame);
    if (result != JFileChooser.APPROVE_OPTION) {
      return Optional.empty();
    }

    return Optional.ofNullable(chooser.getSelectedFile()).map(File::toPath);
  }

  private Optional<Path> normalizeSavePath(Path selected) {
    if (selected == null) {
      return Optional.empty();
    }

    try {
      final String name = selected.getFileName().toString();
      if (!name.toLowerCase().endsWith(".bin")) {
        return Optional.of(selected.resolveSibling(name + ".bin"));
      }
      return Optional.of(selected);
    } catch (InvalidPathException e) {
      return Optional.empty();
    }
  }

  private void saveStateToPath(
    Path targetPath,
    String selectedSheetName,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet
  ) {
    try {
      final Path parent = targetPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      final SaveState state = new SaveState(
        selectedSheetName,
        copyWeightProgress(weightProgressBySheet),
        copyAllAmericanLabels(allAmericanLabelsBySheet)
      );
      try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(targetPath))) {
        output.writeObject(state);
      }
      JOptionPane.showMessageDialog(frame, "Bracket saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
        frame,
        "Failed to save state: " + e.getMessage(),
        "Save Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }

  private boolean loadStateFromPath(
    Path sourcePath,
    List<String> sheetNames,
    Map<String, List<Integer>> weightProgressBySheet,
    Map<String, List<String>> allAmericanLabelsBySheet,
    Consumer<String> loadWeightSheet,
    boolean notify
  ) throws IOException, ClassNotFoundException {
    if (!Files.exists(sourcePath)) {
      return false;
    }

    try (ObjectInputStream input = new SaveStateObjectInputStream(sourcePath)) {
      final SaveState state = (SaveState) input.readObject();
      weightProgressBySheet.clear();
      weightProgressBySheet.putAll(copyWeightProgress(state.weightProgressBySheet));
      allAmericanLabelsBySheet.clear();
      allAmericanLabelsBySheet.putAll(copyAllAmericanLabels(state.allAmericanLabelsBySheet));
      String initialSheet = state.selectedSheetName;
      if (initialSheet == null || !sheetNames.contains(initialSheet)) {
        initialSheet = sheetNames.isEmpty() ? null : sheetNames.get(0);
      }
      if (initialSheet != null) {
        loadWeightSheet.accept(initialSheet);
      }
      if (notify) {
        JOptionPane.showMessageDialog(frame, "Bracket loaded.", "Loaded", JOptionPane.INFORMATION_MESSAGE);
      }
      return true;
    }
  }

  private Map<String, List<Integer>> copyWeightProgress(Map<String, List<Integer>> source) {
    final Map<String, List<Integer>> copy = new HashMap<>();
    for (Map.Entry<String, List<Integer>> entry : source.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }

    return copy;
  }

  private Map<String, List<String>> copyAllAmericanLabels(Map<String, List<String>> source) {
    final Map<String, List<String>> copy = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : source.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }

    return copy;
  }


  private static final class SaveStateObjectInputStream extends ObjectInputStream {
    private SaveStateObjectInputStream(Path sourcePath) throws IOException {
      super(Files.newInputStream(sourcePath));
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      if (LEGACY_CLASS_NAMES.contains(desc.getName())) {
        return SaveState.class;
      }
      return super.resolveClass(desc);
    }
  }
}
