package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Static checks for dashboard navigation and logout handler. */
public class DashboardUiContractTest {

  private static final Path RES_DIR = Path.of("src/main/resources/META-INF/resources");

  private static String readFile(String name) throws IOException {
    return Files.readString(RES_DIR.resolve(name), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Dashboard uses showPage(name, this) for nav items")
  void testShowPageSignature() throws IOException {
    String dashboard = readFile("dashboard.html");
    assertTrue(
        dashboard.contains("showPage('overview', this)")
            && dashboard.contains("showPage('users', this)")
            && dashboard.contains("showPage('datasources', this)"),
        "Dashboard nav items must pass 'this' to showPage");
    assertTrue(
        dashboard.contains("function showPage(pageName, el)"),
        "showPage should accept the clicked element parameter");
  }

  @Test
  @DisplayName("Dashboard logout button calls logout()")
  void testLogoutHandler() throws IOException {
    String dashboard = readFile("dashboard.html");
    assertTrue(
        dashboard.contains("onclick=\"logout()\""), "Logout handler should call logout() function");
  }
}
