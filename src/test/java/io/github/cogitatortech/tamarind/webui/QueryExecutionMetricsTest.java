package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for query execution metrics display in notebook cells */
public class QueryExecutionMetricsTest {

  @Test
  public void testUpdateCellMetricsFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function updateCellMetrics(cellId"),
        "notebook should have updateCellMetrics function");
  }

  @Test
  public void testFormatTimeAgoFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function formatTimeAgo(date)"),
        "notebook should have formatTimeAgo function");
  }

  @Test
  public void testCellMetricsStylesDefined() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains(".cell-metrics"), "notebook should define cell-metrics styles");
    assertTrue(
        notebookHtml.contains(".metric-badge"), "notebook should define metric-badge styles");
  }

  @Test
  public void testMetricBadgeTypes() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for all metric badge types
    assertTrue(notebookHtml.contains(".metric-time"), "notebook should have time metric style");
    assertTrue(notebookHtml.contains(".metric-rows"), "notebook should have rows metric style");
    assertTrue(notebookHtml.contains(".metric-cached"), "notebook should have cached metric style");
    assertTrue(
        notebookHtml.contains(".metric-warning"), "notebook should have warning metric style");
    assertTrue(
        notebookHtml.contains(".metric-timestamp"), "notebook should have timestamp metric style");
    assertTrue(
        notebookHtml.contains(".status-running"), "notebook should have running status style");
  }

  @Test
  public void testMetricsIncludeIcons() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains(".metric-icon"), "metrics should include icon elements");
  }

  @Test
  public void testMetricsUpdateOnQueryExecution() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("updateCellMetrics(cellId)"),
        "metrics should be updated when query executes");
  }

  @Test
  public void testRunningStateDisplayed() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("status: 'running'"),
        "running state should be shown during query execution");
  }

  @Test
  public void testExecutionTimeTracking() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("executionTime:"), "execution time should be tracked in cellResults");
    assertTrue(notebookHtml.contains("executedAt:"), "execution timestamp should be tracked");
  }

  @Test
  public void testTimeFormattingLogic() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for time formatting logic
    assertTrue(notebookHtml.contains("just now"), "should format very recent times as 'just now'");
    assertTrue(notebookHtml.contains("ago"), "should format relative times with 'ago'");
  }

  @Test
  public void testCacheIndicator() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("cached") && notebookHtml.contains("💾"),
        "should show cache indicator with icon");
  }

  @Test
  public void testRowCountDisplay() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("rows.length.toLocaleString()"),
        "row count should be formatted with locale separators");
    assertTrue(notebookHtml.contains("rows"), "row count should be displayed");
  }

  @Test
  public void testTruncatedIndicator() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("truncated"),
        "should show truncated indicator when results are truncated");
  }

  @Test
  public void testMetricsAnimation() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("@keyframes pulse-badge"),
        "running state should have pulse animation");
  }

  @Test
  public void testMetricsResponsive() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("flex-wrap"), "metrics should wrap on small screens");
  }

  @Test
  public void testTooltipsForMetrics() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("title=\"Execution time\""),
        "metrics should have tooltips explaining what they are");
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
