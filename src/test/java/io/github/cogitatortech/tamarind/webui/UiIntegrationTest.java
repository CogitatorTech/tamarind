package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for the complete UI flow
 *
 * <p>Tests verify that the UI pages are properly configured and reference all necessary JavaScript
 * modules.
 */
public class UiIntegrationTest {

  @Test
  public void testIndexHtmlExists() {
    String indexHtml = readResourceFile("/META-INF/resources/index.html");
    assertNotNull(indexHtml, "index.html should exist");
    assertTrue(indexHtml.contains("<!DOCTYPE html>"), "index.html should be valid HTML");
  }

  @Test
  public void testDashboardHtmlExists() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");
    assertNotNull(dashboardHtml, "dashboard.html should exist");
    assertTrue(dashboardHtml.contains("<!DOCTYPE html>"), "dashboard.html should be valid HTML");
  }

  @Test
  public void testNotebookHtmlExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");
    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("<!DOCTYPE html>"), "notebook.html should be valid HTML");
  }

  @Test
  public void testApiClientExists() {
    String apiClientJs = readResourceFile("/META-INF/resources/assets/js/api-client.js");
    assertNotNull(apiClientJs, "api-client.js should exist");
    assertTrue(
        apiClientJs.contains("class TamarindApiClient"),
        "api-client.js should define TamarindApiClient class");
  }

  @Test
  public void testUtilsExists() {
    String utilsJs = readResourceFile("/META-INF/resources/assets/js/utils.js");
    assertNotNull(utilsJs, "utils.js should exist");
    assertTrue(utilsJs.contains("const TamarindUtils"), "utils.js should define TamarindUtils");
  }

  @Test
  public void testDashboardJsExists() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");
    assertNotNull(dashboardJs, "dashboard.js should exist");
    assertTrue(
        dashboardJs.contains("class TamarindDashboard"),
        "dashboard.js should define TamarindDashboard class");
  }

  @Test
  public void testDashboardReferencesAllModules() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");
    assertNotNull(dashboardHtml, "dashboard.html should exist");

    // Check for script references
    assertTrue(
        dashboardHtml.contains("api-client.js"), "dashboard.html should reference api-client.js");
    assertTrue(
        dashboardHtml.contains("dashboard.js"), "dashboard.html should reference dashboard.js");

    // Verify order: api-client before dashboard
    int apiClientIndex = dashboardHtml.indexOf("api-client.js");
    int dashboardJsIndex = dashboardHtml.indexOf("dashboard.js");
    assertTrue(
        apiClientIndex < dashboardJsIndex, "api-client.js should be loaded before dashboard.js");
  }

  @Test
  public void testLoginFormExists() {
    String indexHtml = readResourceFile("/META-INF/resources/index.html");
    assertNotNull(indexHtml, "index.html should exist");

    assertTrue(indexHtml.contains("id=\"loginForm\""), "index.html should have login form");
    assertTrue(indexHtml.contains("id=\"registerForm\""), "index.html should have register form");
    assertTrue(indexHtml.contains("/api/v1/auth/login"), "index.html should call login API");
    assertTrue(indexHtml.contains("/api/v1/auth/register"), "index.html should call register API");
  }

  @Test
  public void testDashboardHasAllSections() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");
    assertNotNull(dashboardHtml, "dashboard.html should exist");

    // Check for main sections
    assertTrue(
        dashboardHtml.contains("id=\"overview-page\""), "dashboard should have overview page");
    assertTrue(dashboardHtml.contains("id=\"users-page\""), "dashboard should have users page");
    assertTrue(
        dashboardHtml.contains("id=\"datasources-page\""),
        "dashboard should have datasources page");
    assertTrue(
        dashboardHtml.contains("id=\"monitoring-page\""), "dashboard should have monitoring page");
    assertTrue(dashboardHtml.contains("id=\"config-page\""), "dashboard should have config page");
  }

  @Test
  public void testNotebookHasCodeMirror() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");
    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for CodeMirror
    assertTrue(notebookHtml.contains("codemirror"), "notebook should include CodeMirror");
    assertTrue(notebookHtml.contains("sql"), "notebook should include SQL mode for CodeMirror");
  }

  @Test
  public void testNotebookHasVega() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");
    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for Vega
    assertTrue(notebookHtml.contains("vega"), "notebook should include Vega for visualizations");
  }

  @Test
  public void testAllPagesHaveFavicon() {
    String indexHtml = readResourceFile("/META-INF/resources/index.html");
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertTrue(indexHtml.contains("/assets/logos/logo.png"), "index.html should have favicon");
    assertTrue(
        dashboardHtml.contains("/assets/logos/logo.png"), "dashboard.html should have favicon");
    assertTrue(
        notebookHtml.contains("/assets/logos/logo.png"), "notebook.html should have favicon");
  }

  @Test
  public void testUtilsHasEssentialFunctions() {
    String utilsJs = readResourceFile("/META-INF/resources/assets/js/utils.js");
    assertNotNull(utilsJs, "utils.js should exist");

    // Check for essential utility functions
    assertTrue(utilsJs.contains("escapeHtml"), "utils should have escapeHtml function");
    assertTrue(utilsJs.contains("debounce"), "utils should have debounce function");
    assertTrue(utilsJs.contains("formatBytes"), "utils should have formatBytes function");
    assertTrue(
        utilsJs.contains("validateIdentifier"), "utils should have validateIdentifier function");
    assertTrue(utilsJs.contains("copyToClipboard"), "utils should have copyToClipboard function");
    assertTrue(utilsJs.contains("downloadFile"), "utils should have downloadFile function");
  }

  @Test
  public void testApiClientHasEssentialMethods() {
    String apiClientJs = readResourceFile("/META-INF/resources/assets/js/api-client.js");
    assertNotNull(apiClientJs, "api-client.js should exist");

    // Check for essential API methods
    assertTrue(apiClientJs.contains("async login("), "API client should have login method");
    assertTrue(apiClientJs.contains("async register("), "API client should have register method");
    assertTrue(
        apiClientJs.contains("async executeQuery("), "API client should have executeQuery method");
    assertTrue(
        apiClientJs.contains("async uploadFile("), "API client should have uploadFile method");
    assertTrue(
        apiClientJs.contains("async listTables("), "API client should have listTables method");
  }

  @Test
  public void testApiClientHandlesAuth() {
    String apiClientJs = readResourceFile("/META-INF/resources/assets/js/api-client.js");
    assertNotNull(apiClientJs, "api-client.js should exist");

    // Check for auth handling
    assertTrue(apiClientJs.contains("getAuthToken"), "API client should have getAuthToken method");
    assertTrue(apiClientJs.contains("clearAuth"), "API client should have clearAuth method");
    assertTrue(
        apiClientJs.contains("response.status === 401"), "API client should handle 401 responses");
  }

  @Test
  public void testDashboardHasFileUpload() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");
    assertNotNull(dashboardHtml, "dashboard.html should exist");

    assertTrue(dashboardHtml.contains("id=\"uploadArea\""), "dashboard should have upload area");
    assertTrue(dashboardHtml.contains("id=\"fileInput\""), "dashboard should have file input");
    assertTrue(
        dashboardHtml.contains("handleFileSelect"), "dashboard should have file select handler");
    assertTrue(dashboardHtml.contains("uploadFile"), "dashboard should have upload function");
  }

  @Test
  public void testDashboardHasS3Registration() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");
    assertNotNull(dashboardHtml, "dashboard.html should exist");

    assertTrue(
        dashboardHtml.contains("id=\"s3TableName\""), "dashboard should have S3 table name input");
    assertTrue(dashboardHtml.contains("id=\"s3Path\""), "dashboard should have S3 path input");
    assertTrue(
        dashboardHtml.contains("registerS3Table"),
        "dashboard should have S3 registration function");
  }

  @Test
  public void testNotebookHasEscapeHtmlFunction() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");
    assertNotNull(notebookHtml, "notebook.html should exist");

    assertTrue(
        notebookHtml.contains("function escapeHtml(text)"),
        "notebook should have escapeHtml function");
  }

  @Test
  public void testUtilsIsGloballyAvailable() {
    String utilsJs = readResourceFile("/META-INF/resources/assets/js/utils.js");
    assertNotNull(utilsJs, "utils.js should exist");

    assertTrue(
        utilsJs.contains("window.TamarindUtils = TamarindUtils"),
        "utils should be available globally");
  }

  private String readResourceFile(String path) {
    try {
      var resource = getClass().getResourceAsStream(path);
      if (resource == null) {
        return null;
      }
      return new String(resource.readAllBytes());
    } catch (Exception e) {
      return null;
    }
  }
}
