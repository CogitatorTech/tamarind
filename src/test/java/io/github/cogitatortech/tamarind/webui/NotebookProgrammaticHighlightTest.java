package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Static assertions for programmatic editor updates with CodeMirror. */
public class NotebookProgrammaticHighlightTest {
  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String read(String filename) throws IOException {
    return Files.readString(RES_DIR.resolve(filename), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("insertQuery uses editorInstances and setValue; runSelection present")
  void testProgrammaticEditorHooks() throws IOException {
    String html = read("notebook.html");
    assertAll(
        () ->
            assertTrue(
                html.contains("editorInstances[lastCell.id]"),
                "insertQuery should use editorInstances of last cell"),
        () -> assertTrue(html.contains("setValue("), "insertQuery should call setValue on editor"),
        () -> assertTrue(html.contains("runSelection()"), "runSelection function should exist"),
        () ->
            assertTrue(
                html.contains("getSelection()") || html.contains("cm.getSelection()"),
                "runSelection should query the current selection"));
  }
}
