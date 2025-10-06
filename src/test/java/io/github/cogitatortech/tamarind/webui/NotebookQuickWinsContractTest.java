package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contract tests to ensure quick-win UI assets are present in notebook.html. */
public class NotebookQuickWinsContractTest {
  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String read(String filename) throws IOException {
    return Files.readString(RES_DIR.resolve(filename), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Notebook includes sql-formatter and Vega scripts, and has History/Schema UI hooks")
  void testAssetsAndHooksPresent() throws IOException {
    String html = read("notebook.html");
    assertAll(
        () -> assertTrue(html.contains("sql-formatter.min.js"), "sql-formatter should be loaded"),
        () -> assertTrue(html.contains("vega@5"), "vega should be loaded"),
        () -> assertTrue(html.contains("vega-lite@5"), "vega-lite should be loaded"),
        () -> assertTrue(html.contains("vega-embed@6"), "vega-embed should be loaded"),
        () -> assertTrue(html.contains("history-section"), "History section present"),
        () -> assertTrue(html.contains("schemaModal"), "Schema modal present"),
        () -> assertTrue(html.contains("visualizeResult"), "Visualize function present"),
        () -> assertTrue(html.contains("toggleColumnsMenu"), "Columns toggle present"));
  }
}
