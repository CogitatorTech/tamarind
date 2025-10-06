package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for error handler functionality */
public class ErrorHandlerTest {

  @Test
  public void testErrorHandlerExists() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(
        errorHandlerJs.contains("const ErrorHandler"),
        "error-handler.js should define ErrorHandler");
    assertTrue(
        errorHandlerJs.contains("window.ErrorHandler = ErrorHandler"),
        "ErrorHandler should be globally available");
  }

  @Test
  public void testErrorMapExists() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(errorHandlerJs.contains("errorMap"), "ErrorHandler should have errorMap");

    // Check for common error codes
    assertTrue(errorHandlerJs.contains("'SQL_ERROR'"), "ErrorHandler should handle SQL_ERROR");
    assertTrue(
        errorHandlerJs.contains("'NETWORK_ERROR'"), "ErrorHandler should handle NETWORK_ERROR");
    assertTrue(
        errorHandlerJs.contains("'RATE_LIMIT_EXCEEDED'"),
        "ErrorHandler should handle RATE_LIMIT_EXCEEDED");
    assertTrue(
        errorHandlerJs.contains("'UNAUTHORIZED'"), "ErrorHandler should handle UNAUTHORIZED");
  }

  @Test
  public void testErrorHandlerHasSuggestions() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(errorHandlerJs.contains("suggestions"), "Errors should include suggestions");
  }

  @Test
  public void testErrorHandlerHasIcons() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(
        errorHandlerJs.contains("icon"), "Errors should include icons for visual identification");
  }

  @Test
  public void testErrorHandlerIntegratedWithApiClient() {
    String apiClientJs = readResourceFile("/META-INF/resources/assets/js/api-client.js");

    assertNotNull(apiClientJs, "api-client.js should exist");
    assertTrue(
        apiClientJs.contains("ErrorHandler"), "API client should integrate with ErrorHandler");
    assertTrue(
        apiClientJs.contains("ErrorHandler.handle"),
        "API client should call ErrorHandler.handle on errors");
  }

  @Test
  public void testDashboardReferencesErrorHandler() {
    String dashboardHtml = readResourceFile("/META-INF/resources/dashboard.html");

    assertNotNull(dashboardHtml, "dashboard.html should exist");
    assertTrue(
        dashboardHtml.contains("error-handler.js"),
        "dashboard.html should reference error-handler.js");

    // Verify load order: utils -> error-handler -> api-client -> dashboard
    int utilsIndex = dashboardHtml.indexOf("utils.js");
    int errorHandlerIndex = dashboardHtml.indexOf("error-handler.js");
    int apiClientIndex = dashboardHtml.indexOf("api-client.js");
    int dashboardIndex = dashboardHtml.indexOf("dashboard.js");

    assertTrue(utilsIndex < errorHandlerIndex, "utils.js should load before error-handler.js");
    assertTrue(
        errorHandlerIndex < apiClientIndex, "error-handler.js should load before api-client.js");
    assertTrue(apiClientIndex < dashboardIndex, "api-client.js should load before dashboard.js");
  }

  @Test
  public void testErrorHandlerHasStyles() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(errorHandlerJs.contains("addStyles"), "ErrorHandler should have addStyles method");
    assertTrue(errorHandlerJs.contains(".error-modal"), "ErrorHandler should define modal styles");
  }

  @Test
  public void testErrorHandlerHasHandleMethod() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(errorHandlerJs.contains("handle(error)"), "ErrorHandler should have handle method");
  }

  @Test
  public void testErrorHandlerHasShowMethod() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(errorHandlerJs.contains("show(errorInfo"), "ErrorHandler should have show method");
  }

  @Test
  public void testErrorHandlerSupportsRetry() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(errorHandlerJs.contains("retry"), "ErrorHandler should support retry functionality");
    assertTrue(
        errorHandlerJs.contains("shouldShowRetry"),
        "ErrorHandler should have logic for showing retry button");
  }

  @Test
  public void testErrorHandlerHasAccessibility() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(
        errorHandlerJs.contains("aria-label"),
        "ErrorHandler should include ARIA labels for accessibility");
    assertTrue(
        errorHandlerJs.contains("focus()"),
        "ErrorHandler should manage focus for keyboard navigation");
  }

  @Test
  public void testErrorHandlerResponsive() {
    String errorHandlerJs = readResourceFile("/META-INF/resources/assets/js/error-handler.js");

    assertNotNull(errorHandlerJs, "error-handler.js should exist");
    assertTrue(
        errorHandlerJs.contains("@media"),
        "ErrorHandler styles should include responsive breakpoints");
    assertTrue(errorHandlerJs.contains("576px"), "ErrorHandler should have mobile breakpoint");
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
