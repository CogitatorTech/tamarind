package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for dashboard UI bug fixes
 *
 * <p>Tests verify that all functions referenced by inline event handlers are properly defined and
 * that the file upload functionality works correctly.
 */
public class DashboardBugFixesTest {

  @Test
  public void testDashboardHtmlContainsRequiredFunctions() {
    // Read dashboard.html and verify it contains references to all required functions
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");

    assertNotNull(dashboardHtml, "dashboard.html should be readable");

    // Verify onclick handler references
    assertTrue(
        dashboardHtml.contains("onclick=\"refreshUsers()\""),
        "dashboard.html should contain refreshUsers() onclick handler");
    assertTrue(
        dashboardHtml.contains("onclick=\"refreshTables()\""),
        "dashboard.html should contain refreshTables() onclick handler");
    assertTrue(
        dashboardHtml.contains("onclick=\"refreshMonitoring()\""),
        "dashboard.html should contain refreshMonitoring() onclick handler");
    assertTrue(
        dashboardHtml.contains("onclick=\"refreshConfig()\""),
        "dashboard.html should contain refreshConfig() onclick handler");
    assertTrue(
        dashboardHtml.contains("onchange=\"handleFileSelect(event)\""),
        "dashboard.html should contain handleFileSelect() onchange handler");
    assertTrue(
        dashboardHtml.contains("onclick=\"uploadFile()\""),
        "dashboard.html should contain uploadFile() onclick handler");
    assertTrue(
        dashboardHtml.contains("onclick=\"registerS3Table()\""),
        "dashboard.html should contain registerS3Table() onclick handler");
  }

  @Test
  public void testDashboardJsContainsAllFunctions() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");

    assertNotNull(dashboardJs, "dashboard.js should be readable");

    // Verify all required functions are defined
    assertTrue(
        dashboardJs.contains("function refreshUsers()"),
        "dashboard.js should define refreshUsers()");
    assertTrue(
        dashboardJs.contains("function refreshTables()"),
        "dashboard.js should define refreshTables()");
    assertTrue(
        dashboardJs.contains("function refreshMonitoring()"),
        "dashboard.js should define refreshMonitoring()");
    assertTrue(
        dashboardJs.contains("function refreshConfig()"),
        "dashboard.js should define refreshConfig()");
    assertTrue(
        dashboardJs.contains("function handleFileSelect("),
        "dashboard.js should define handleFileSelect()");
    assertTrue(
        dashboardJs.contains("async function uploadFile()"),
        "dashboard.js should define uploadFile()");
    assertTrue(
        dashboardJs.contains("async function registerS3Table()"),
        "dashboard.js should define registerS3Table()");
  }

  @Test
  public void testApiClientExists() {
    String apiClientJs = readResourceFile("/META-INF/resources/assets/js/api-client.js");

    assertNotNull(apiClientJs, "api-client.js should exist");
    assertTrue(
        apiClientJs.contains("class TamarindApiClient"),
        "api-client.js should define TamarindApiClient class");
    assertTrue(
        apiClientJs.contains("async uploadFile(file, tableName)"),
        "api-client.js should have uploadFile method");
    assertTrue(
        apiClientJs.contains("async registerS3Table("),
        "api-client.js should have registerS3Table method");
  }

  @Test
  public void testDashboardReferencesApiClient() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");

    assertNotNull(dashboardHtml, "dashboard.html should be readable");
    assertTrue(
        dashboardHtml.contains("api-client.js"), "dashboard.html should reference api-client.js");

    // Verify api-client.js is loaded before dashboard.js
    int apiClientIndex = dashboardHtml.indexOf("api-client.js");
    int dashboardJsIndex = dashboardHtml.indexOf("dashboard.js");
    assertTrue(
        apiClientIndex < dashboardJsIndex, "api-client.js should be loaded before dashboard.js");
  }

  @Test
  public void testFileUploadValidation() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");

    // Verify file upload includes validation
    assertTrue(
        dashboardJs.contains("if (!selectedFile)"),
        "uploadFile should validate that a file is selected");
    assertTrue(
        dashboardJs.contains("Please select a file first"),
        "uploadFile should show error message when no file is selected");
  }

  @Test
  public void testS3TableValidation() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");

    // Verify S3 registration includes validation
    assertTrue(
        dashboardJs.contains("if (!tableName)"), "registerS3Table should validate table name");
    assertTrue(dashboardJs.contains("if (!s3Path)"), "registerS3Table should validate S3 path");
    assertTrue(
        dashboardJs.contains("s3Path.startsWith('s3://')"),
        "registerS3Table should validate S3 path format");
  }

  @Test
  public void testDragAndDropSupport() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");

    // Verify drag and drop handlers are set up
    assertTrue(
        dashboardJs.contains("addEventListener('dragover'"),
        "dashboard.js should handle dragover event");
    assertTrue(
        dashboardJs.contains("addEventListener('dragleave'"),
        "dashboard.js should handle dragleave event");
    assertTrue(
        dashboardJs.contains("addEventListener('drop'"), "dashboard.js should handle drop event");
  }

  @Test
  public void testErrorHandling() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");

    // Verify proper error handling in async functions
    assertTrue(
        dashboardJs.contains("try {") && dashboardJs.contains("} catch (error) {"),
        "async functions should have try-catch error handling");
    assertTrue(
        dashboardJs.contains("dashboard.showAlert"), "errors should be displayed using showAlert");
  }

  @Test
  public void testAuthHandling() {
    String dashboardJs = readResourceFile("/META-INF/resources/assets/js/dashboard.js");

    // Verify 401 responses redirect to login
    int count = countOccurrences(dashboardJs, "response.status === 401");
    assertTrue(count >= 2, "dashboard.js should check for 401 status in multiple places");
    assertTrue(
        dashboardJs.contains("window.location.href = '/index.html'"),
        "401 responses should redirect to login");
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

  private int countOccurrences(String text, String substring) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(substring, index)) != -1) {
      count++;
      index += substring.length();
    }
    return count;
  }
}
