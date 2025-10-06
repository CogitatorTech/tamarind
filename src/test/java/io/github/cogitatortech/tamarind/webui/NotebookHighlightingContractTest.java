package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ensures notebook has CodeMirror highlighting and cleanup hooks to avoid memory leaks. */
public class NotebookHighlightingContractTest {
  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String read(String filename) throws IOException {
    return Files.readString(RES_DIR.resolve(filename), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Notebook includes CodeMirror CSS/JS and editor initialization")
  void testCodeMirrorIncludesAndInit() throws IOException {
    String html = read("notebook.html");
    assertAll(
        () -> assertTrue(html.contains("codemirror.min.css"), "CodeMirror CSS missing"),
        () -> assertTrue(html.contains("codemirror.min.js"), "CodeMirror JS missing"),
        () -> assertTrue(html.contains("mode/sql/sql.min.js"), "SQL mode missing"),
        () -> assertTrue(html.contains("addon/hint/sql-hint.min.js"), "SQL hint missing"),
        () -> assertTrue(html.contains("CodeMirror.fromTextArea"), "CodeMirror init missing"),
        () -> assertTrue(html.contains("editorInstances"), "editorInstances map should exist"));
  }

  @Test
  @DisplayName("Notebook disposes editor instances on delete/clear")
  void testEditorCleanupExists() throws IOException {
    String html = read("notebook.html");
    assertAll(
        () -> assertTrue(html.contains("dispose()"), "deleteCell should dispose editor"),
        () ->
            assertTrue(
                html.contains("Object.values(editorInstances)"),
                "clearNotebook should iterate editors"),
        () ->
            assertTrue(
                html.contains("inst.dispose()"), "clearNotebook should dispose all editors"));
  }
}
