package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verifies favicon and header logo references exist across pages. */
public class LogoIntegrationTest {
  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String read(String filename) throws IOException {
    return Files.readString(RES_DIR.resolve(filename), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("index.html has favicon links and img logo")
  void testIndexLogo() throws IOException {
    String html = read("index.html");
    assertTrue(
        html.contains("/assets/logos/logo.png"), "index should reference /assets/logos/logo.png");
    assertTrue(html.contains("rel=\"icon\""), "index should include favicon");
  }

  @Test
  @DisplayName("dashboard.html has favicon and header logo")
  void testDashboardLogo() throws IOException {
    String html = read("dashboard.html");
    assertTrue(
        html.contains("/assets/logos/logo.png"),
        "dashboard should reference /assets/logos/logo.png");
    assertTrue(html.contains("rel=\"icon\""), "dashboard should include favicon");
  }

  @Test
  @DisplayName("notebook.html has favicon and header logo")
  void testNotebookLogo() throws IOException {
    String html = read("notebook.html");
    assertTrue(
        html.contains("/assets/logos/logo.png"),
        "notebook should reference /assets/logos/logo.png");
    assertTrue(html.contains("rel=\"icon\""), "notebook should include favicon");
  }
}
