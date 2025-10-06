package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for Column Statistics feature
 *
 * <p>Verifies that column headers are clickable and show detailed statistics including min/max,
 * null count, distinct values, and histograms.
 */
public class ColumnStatisticsTest {

  @Test
  public void testShowColumnStatsFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("async function showColumnStats(cellId, columnName)"),
        "notebook should define showColumnStats function");
  }

  @Test
  public void testColumnHeadersAreClickable() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("clickable-header"),
        "Column headers should have clickable-header class");
    assertTrue(
        notebookHtml.contains("onclick=\"showColumnStats"),
        "Column headers should have onclick handler");
  }

  @Test
  public void testComputeColumnStatisticsFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function computeColumnStatistics(columnName, values)"),
        "Should have function to compute column statistics");
  }

  @Test
  public void testStatsIncludeBasicMetrics() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for basic statistics
    assertTrue(
        notebookHtml.contains("total:") && notebookHtml.contains("values.length"),
        "Should compute total count");
    assertTrue(notebookHtml.contains("nullCount"), "Should compute null count");
    assertTrue(notebookHtml.contains("distinctCount"), "Should compute distinct count");
  }

  @Test
  public void testStatsHandleNullValues() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("v !== null && v !== undefined"),
        "Should filter out null and undefined values");
    assertTrue(notebookHtml.contains("nullPercent"), "Should calculate null percentage");
  }

  @Test
  public void testStatsDetectType() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("stats.type = 'numeric'"), "Should detect numeric type");
    assertTrue(notebookHtml.contains("stats.type = 'string'"), "Should detect string type");
    assertTrue(notebookHtml.contains("stats.type = 'boolean'"), "Should detect boolean type");
    assertTrue(notebookHtml.contains("stats.type = 'date'"), "Should detect date type");
  }

  @Test
  public void testNumericStatistics() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("stats.min = Math.min"), "Should calculate min for numeric columns");
    assertTrue(
        notebookHtml.contains("stats.max = Math.max"), "Should calculate max for numeric columns");
    assertTrue(notebookHtml.contains("stats.mean"), "Should calculate mean for numeric columns");
    assertTrue(
        notebookHtml.contains("stats.median"), "Should calculate median for numeric columns");
  }

  @Test
  public void testHistogramGeneration() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function createHistogram(numbers, bins"),
        "Should have histogram creation function");
    assertTrue(
        notebookHtml.contains("stats.histogram = createHistogram"),
        "Should create histogram for numeric columns");
  }

  @Test
  public void testStringStatistics() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("minLength") && notebookHtml.contains("maxLength"),
        "Should calculate min/max length for strings");
    assertTrue(notebookHtml.contains("avgLength"), "Should calculate average length for strings");
  }

  @Test
  public void testTopValuesCalculation() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("topValues"), "Should calculate top values");
    assertTrue(notebookHtml.contains("valueCounts"), "Should count value frequencies");
    assertTrue(notebookHtml.contains(".slice(0, 10)"), "Should limit to top 10 values");
  }

  @Test
  public void testStatsModalStructure() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("columnStatsModal"), "Should create stats modal");
    assertTrue(notebookHtml.contains("statsColumnName"), "Modal should show column name");
    assertTrue(notebookHtml.contains("statsContent"), "Modal should have content area");
  }

  @Test
  public void testStatsModalClosing() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function closeColumnStatsModal"),
        "Should have function to close stats modal");
    assertTrue(
        notebookHtml.contains("onclick=\"closeColumnStatsModal"),
        "Modal should have close handler");
  }

  @Test
  public void testRenderColumnStatsFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function renderColumnStats(stats)"),
        "Should have function to render statistics");
  }

  @Test
  public void testStatsGridDisplay() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("stats-grid"), "Should have grid layout for stats");
    assertTrue(notebookHtml.contains("stat-card"), "Should have card components for each stat");
    assertTrue(notebookHtml.contains("stat-label"), "Stat cards should have labels");
    assertTrue(notebookHtml.contains("stat-value"), "Stat cards should have values");
  }

  @Test
  public void testHistogramVisualization() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("histogram"), "Should have histogram visualization");
    assertTrue(notebookHtml.contains("histogram-bar"), "Should have histogram bars");
    assertTrue(notebookHtml.contains("histogram-label"), "Histogram bars should have labels");
  }

  @Test
  public void testTopValuesTable() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("stats-table"), "Should have table for top values");
    assertTrue(notebookHtml.contains("percent-bar"), "Should visualize percentages with bars");
  }

  @Test
  public void testClickableHeaderStyles() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains(".clickable-header"), "Should define clickable-header style");
    assertTrue(
        notebookHtml.contains("cursor: pointer"), "Clickable headers should show pointer cursor");
    assertTrue(
        notebookHtml.contains(".clickable-header:hover"),
        "Clickable headers should have hover state");
  }

  @Test
  public void testStatsModalStyles() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains(".column-stats-modal"), "Should define stats modal styles");
    assertTrue(
        notebookHtml.contains("max-width: 900px"), "Stats modal should have appropriate max width");
  }

  @Test
  public void testWarningForNulls() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("stat-value.warning"), "Should have warning style for null values");
    assertTrue(
        notebookHtml.contains("stats.nullCount > 0 ? 'warning' : ''"),
        "Should apply warning class when nulls are present");
  }

  @Test
  public void testErrorHandling() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Find the showColumnStats function
    int funcStart = notebookHtml.indexOf("async function showColumnStats");
    int funcEnd = notebookHtml.indexOf("function closeColumnStatsModal", funcStart);
    String statsFunction = notebookHtml.substring(funcStart, funcEnd);

    assertTrue(
        statsFunction.contains("try {") && statsFunction.contains("} catch"),
        "showColumnStats should have error handling");
    assertTrue(
        statsFunction.contains("Error computing statistics"),
        "Should show error message on failure");
  }

  @Test
  public void testDataAvailabilityCheck() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    int funcStart = notebookHtml.indexOf("async function showColumnStats");
    int funcEnd = notebookHtml.indexOf("function closeColumnStatsModal", funcStart);
    String statsFunction = notebookHtml.substring(funcStart, funcEnd);

    assertTrue(
        statsFunction.contains("if (!saved || !saved.rows"), "Should check if data is available");
    assertTrue(
        statsFunction.contains("No data available"),
        "Should show message when no data is available");
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
