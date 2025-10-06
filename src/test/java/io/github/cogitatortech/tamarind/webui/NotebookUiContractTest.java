package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lightweight UI contract checks against notebook.html to prevent regressions. These tests do not
 * render HTML; they statically verify key strings and flows exist.
 */
public class NotebookUiContractTest {

  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String readFile(String name) throws IOException {
    return Files.readString(RES_DIR.resolve(name), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Notebook has Dashboard navigation button and template loader")
  void testDashboardButtonAndTemplateLoader() throws IOException {
    String notebook = readFile("notebook.html");
    assertTrue(
        notebook.contains("Dashboard</button>"),
        "Notebook should contain Dashboard navigation button");
    assertTrue(
        notebook.contains("tamarind_query_template"),
        "Notebook should reference tamarind_query_template");
    assertTrue(
        notebook.contains("localStorage.removeItem('tamarind_query_template')"),
        "Notebook should clear the template after applying");
  }

  @Test
  @DisplayName("Notebook lists tables via /api/v1/datasources/tables")
  void testTablesEndpoint() throws IOException {
    String notebook = readFile("notebook.html");
    assertTrue(
        notebook.contains("/api/v1/datasources/tables"),
        "Notebook should call the correct tables endpoint");
  }

  @Test
  @DisplayName("Notebook robust result parsing exists (content-type, parse error, non-JSON)")
  void testRobustParsingMarkers() throws IOException {
    String notebook = readFile("notebook.html");
    assertAll(
        () -> assertTrue(notebook.contains("content-type"), "Should check content-type"),
        () -> assertTrue(notebook.contains("Response parse error"), "Should show parse error UI"),
        () -> assertTrue(notebook.contains("Unexpected response"), "Should show non-JSON UI"));
  }

  @Test
  @DisplayName("Notebook shows cached/truncated badges and execution time")
  void testMetadataBadges() throws IOException {
    String notebook = readFile("notebook.html");
    assertTrue(notebook.contains("badges.push('cached')"), "Should render cached badge");
    assertTrue(notebook.contains("badges.push('truncated')"), "Should render truncated badge");
    assertTrue(
        notebook.contains("executionTimeMs") || notebook.contains("executionTime"),
        "Should use metadata execution time when present");
  }
}
