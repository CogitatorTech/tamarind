package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ensures CodeMirror is wired for SQL editing in the notebook. */
public class NotebookCodeMirrorContractTest {
  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String read(String filename) throws IOException {
    return Files.readString(RES_DIR.resolve(filename), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Notebook uses CodeMirror with SQL mode and hint")
  void testNotebookHasCodeMirror() throws IOException {
    String html = read("notebook.html");
    assertAll(
        () -> assertTrue(html.contains("codemirror.min.css"), "CodeMirror CSS present"),
        () -> assertTrue(html.contains("codemirror.min.js"), "CodeMirror JS present"),
        () -> assertTrue(html.contains("mode/sql/sql.min.js"), "SQL mode present"),
        () -> assertTrue(html.contains("addon/hint/sql-hint.min.js"), "SQL hint present"),
        () -> assertTrue(html.contains("CodeMirror.fromTextArea"), "CodeMirror initialized"),
        () ->
            assertTrue(
                html.contains("extraKeys") && html.contains("Shift-Enter"),
                "Shift-Enter configured"));
  }
}
