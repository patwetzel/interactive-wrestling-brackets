package wrestler;

import static constants.Constants.SHEET_NS;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import team.Team;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SeededWrestlerImporter {
  private static final String SHEET_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

  public List<String> readSheetNames(Path filePath) throws Exception {
    try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
      List<SheetMetadata> sheets = readSheetMetadata(zipFile);
      List<String> names = new ArrayList<>();
      for (SheetMetadata sheet : sheets) {
        names.add(sheet.name());
      }
      return names;
    }
  }

  public List<Wrestler> readSeededWrestlers(Path filePath) throws Exception {
    List<String> names = readSheetNames(filePath);
    if (names.isEmpty()) {
      throw new IllegalStateException("No sheets found in workbook.");
    }
    return readSeededWrestlers(filePath, names.get(0));
  }

  public List<Wrestler> readSeededWrestlers(Path filePath, String sheetName) throws Exception {
    try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
      List<SheetMetadata> sheets = readSheetMetadata(zipFile);
      String worksheetEntry = null;
      for (SheetMetadata sheet : sheets) {
        if (sheet.name().equals(sheetName)) {
          worksheetEntry = sheet.entryName();
          break;
        }
      }

      if (worksheetEntry == null) {
        throw new IllegalArgumentException("Sheet not found: " + sheetName);
      }

      List<String> sharedStrings = readSharedStrings(zipFile);
      Document sheet = readXmlEntry(zipFile, worksheetEntry);

      NodeList rowNodes = sheet.getElementsByTagNameNS(SHEET_NS, "row");
      if (rowNodes.getLength() == 0) {
        throw new IllegalStateException("No rows found in worksheet: " + sheetName);
      }

      Element headerRow = (Element) rowNodes.item(0);
      Map<String, String> headerToColumn = extractHeaderToColumnMap(headerRow, sharedStrings);

      String seedColumn = headerToColumn.get("seed");
      String wrestlerColumn = headerToColumn.get("wrestler");
      String teamColumn = headerToColumn.get("team");
      String winsColumn = headerToColumn.get("wins");
      String lossesColumn = headerToColumn.get("losses");

      if (seedColumn == null || wrestlerColumn == null || teamColumn == null || winsColumn == null || lossesColumn == null) {
        throw new IllegalStateException("Expected headers: Seed, Wrestler, Team, Wins, Losses.");
      }

      List<Wrestler> result = new ArrayList<>();
      for (int i = 1; i < rowNodes.getLength(); i++) {
        Element row = (Element) rowNodes.item(i);
        Map<String, String> cells = extractCellValues(row, sharedStrings);

        String name = cells.getOrDefault(wrestlerColumn, "").trim();
        String team = cells.getOrDefault(teamColumn, "").trim();
        if (name.isBlank() && team.isBlank()) {
          continue;
        }

        int seed = parseIntegerOrZero(cells.getOrDefault(seedColumn, "0"));
        int wins = parseIntegerOrZero(cells.getOrDefault(winsColumn, "0"));
        int losses = parseIntegerOrZero(cells.getOrDefault(lossesColumn, "0"));

        Wrestler wrestler = new Wrestler(name, new Team(team), seed, wins, losses);
        result.add(wrestler);
      }

      return result;
    }
  }

  private List<SheetMetadata> readSheetMetadata(ZipFile zipFile) throws Exception {
    Document workbook = readXmlEntry(zipFile, "xl/workbook.xml");
    Document relationships = readXmlEntry(zipFile, "xl/_rels/workbook.xml.rels");

    Map<String, String> relationshipTargets = new HashMap<>();
    NodeList relationshipNodes = relationships.getElementsByTagNameNS("*", "Relationship");
    for (int i = 0; i < relationshipNodes.getLength(); i++) {
      Element relationship = (Element) relationshipNodes.item(i);
      String id = relationship.getAttribute("Id");
      String target = relationship.getAttribute("Target");
      if (!id.isBlank() && !target.isBlank()) {
        relationshipTargets.put(id, normalizeWorksheetEntry(target));
      }
    }

    List<SheetMetadata> sheets = new ArrayList<>();
    NodeList sheetNodes = workbook.getElementsByTagNameNS(SHEET_NS, "sheet");
    for (int i = 0; i < sheetNodes.getLength(); i++) {
      Element sheetElement = (Element) sheetNodes.item(i);
      String name = sheetElement.getAttribute("name");
      if (name.isBlank()) {
        continue;
      }

      String relationshipId = sheetElement.getAttributeNS(SHEET_REL_NS, "id");
      String entryName = relationshipTargets.get(relationshipId);
      if (entryName == null || entryName.isBlank()) {
        entryName = "xl/worksheets/sheet" + (i + 1) + ".xml";
      }
      sheets.add(new SheetMetadata(name, entryName));
    }

    if (sheets.isEmpty()) {
      throw new IllegalStateException("No sheets found in workbook.");
    }

    return sheets;
  }

  private String normalizeWorksheetEntry(String target) {
    String normalized = target.replace('\\', '/');
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }

    if (!normalized.startsWith("xl/")) {
      normalized = "xl/" + normalized;
    }

    return normalized;
  }

  private Map<String, String> extractHeaderToColumnMap(Element headerRow, List<String> sharedStrings) {
    Map<String, String> headers = new HashMap<>();
    Map<String, String> values = extractCellValues(headerRow, sharedStrings);
    for (Map.Entry<String, String> entry : values.entrySet()) {
      headers.put(entry.getValue().trim().toLowerCase(), entry.getKey());
    }
    return headers;
  }

  private Map<String, String> extractCellValues(Element row, List<String> sharedStrings) {
    Map<String, String> cells = new HashMap<>();
    NodeList cellNodes = row.getElementsByTagNameNS(SHEET_NS, "c");

    for (int i = 0; i < cellNodes.getLength(); i++) {
      Element cell = (Element) cellNodes.item(i);
      String reference = cell.getAttribute("r");
      String column = extractColumn(reference);
      if (column.isBlank()) {
        continue;
      }
      cells.put(column, readCellValue(cell, sharedStrings));
    }

    return cells;
  }

  private String extractColumn(String cellReference) {
    StringBuilder column = new StringBuilder();
    for (int i = 0; i < cellReference.length(); i++) {
      char c = cellReference.charAt(i);
      if (Character.isLetter(c)) {
        column.append(c);
      } else {
        break;
      }
    }
    return column.toString();
  }

  private String readCellValue(Element cell, List<String> sharedStrings) {
    String type = cell.getAttribute("t");

    if ("inlineStr".equals(type)) {
      NodeList textNodes = cell.getElementsByTagNameNS(SHEET_NS, "t");
      return textNodes.getLength() > 0 ? textNodes.item(0).getTextContent() : "";
    }

    NodeList valueNodes = cell.getElementsByTagNameNS(SHEET_NS, "v");
    if (valueNodes.getLength() == 0) {
      return "";
    }

    String value = valueNodes.item(0).getTextContent();
    if ("s".equals(type)) {
      int index = parseIntegerOrZero(value);
      return (index >= 0 && index < sharedStrings.size()) ? sharedStrings.get(index) : "";
    }

    return value;
  }

  private List<String> readSharedStrings(ZipFile zipFile) throws Exception {
    ZipEntry sharedStringsEntry = zipFile.getEntry("xl/sharedStrings.xml");
    if (sharedStringsEntry == null) {
      return new ArrayList<>();
    }

    Document sharedStringsDoc = readXmlEntry(zipFile, "xl/sharedStrings.xml");
    NodeList textNodes = sharedStringsDoc.getElementsByTagNameNS(SHEET_NS, "t");
    List<String> sharedStrings = new ArrayList<>();
    for (int i = 0; i < textNodes.getLength(); i++) {
      Node textNode = textNodes.item(i);
      sharedStrings.add(textNode.getTextContent());
    }
    return sharedStrings;
  }

  private Document readXmlEntry(ZipFile zipFile, String entryName) throws Exception {
    ZipEntry entry = zipFile.getEntry(entryName);
    if (entry == null) {
      throw new IOException("Missing required entry in .xlsx: " + entryName);
    }

    try (InputStream inputStream = zipFile.getInputStream(entry)) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      return factory.newDocumentBuilder().parse(inputStream);
    }
  }

  private int parseIntegerOrZero(String text) {
    try {
      return (int) Math.max(0, Math.round(Double.parseDouble(text.trim())));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private record SheetMetadata(String name, String entryName) { }
}
