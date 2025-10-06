package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for Quick Data Preview feature
 *
 * <p>Verifies that hover tooltips show sample data from tables
 */
public class QuickDataPreviewTest {

  @Test
  public void testDataPreviewObjectExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("const DataPreview"), "notebook should define DataPreview object");
  }

  @Test
  public void testDataPreviewMethods() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for essential methods
    assertTrue(
        notebookHtml.contains("show(tableName, element)"), "DataPreview should have show method");
    assertTrue(notebookHtml.contains("hide()"), "DataPreview should have hide method");
    assertTrue(
        notebookHtml.contains("fetchPreview(tableName)"),
        "DataPreview should have fetchPreview method");
    assertTrue(
        notebookHtml.contains("render(preview, element, tableName)"),
        "DataPreview should have render method");
  }

  @Test
  public void testDebouncing() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("setTimeout"), "DataPreview should use setTimeout for debouncing");
    assertTrue(notebookHtml.contains("500"), "DataPreview should have 500ms debounce delay");
    assertTrue(notebookHtml.contains("clearTimeout"), "DataPreview should clear timeout on hide");
  }

  @Test
  public void testCaching() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("cache:"), "DataPreview should have cache object");
    assertTrue(
        notebookHtml.contains("this.cache[tableName]"),
        "DataPreview should check cache before fetching");
    assertTrue(notebookHtml.contains("5 * 60 * 1000"), "DataPreview should cache for 5 minutes");
  }

  @Test
  public void testHoverEventHandlers() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("addEventListener('mouseenter'"),
        "Table items should have mouseenter handler");
    assertTrue(
        notebookHtml.contains("addEventListener('mouseleave'"),
        "Table items should have mouseleave handler");
    assertTrue(
        notebookHtml.contains("DataPreview.show"), "mouseenter should call DataPreview.show");
    assertTrue(
        notebookHtml.contains("DataPreview.hide"), "mouseleave should call DataPreview.hide");
  }

  @Test
  public void testPreviewQuery() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("SELECT * FROM ${tableName} LIMIT 5"),
        "DataPreview should query 5 rows");
  }

  @Test
  public void testTooltipRendering() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("data-preview-tooltip"),
        "Should create tooltip with data-preview-tooltip class");
    assertTrue(notebookHtml.contains("preview-header"), "Tooltip should have header");
    assertTrue(notebookHtml.contains("preview-table-wrapper"), "Tooltip should have table wrapper");
    assertTrue(notebookHtml.contains("preview-footer"), "Tooltip should have footer");
  }

  @Test
  public void testRowCountDisplay() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("preview-count"), "Should display row count");
    assertTrue(
        notebookHtml.contains("toLocaleString()"), "Row count should be formatted with locale");
  }

  @Test
  public void testNullValueHandling() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("null-value"), "Should have special styling for NULL values");
    assertTrue(
        notebookHtml.contains("val === null || val === undefined"),
        "Should check for null and undefined");
  }

  @Test
  public void testObjectValueHandling() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("json-value"), "Should have special styling for object values");
    assertTrue(notebookHtml.contains("typeof val === 'object'"), "Should check for object types");
  }

  @Test
  public void testTextTruncation() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("str.length > 50"), "Should truncate long text values");
    assertTrue(
        notebookHtml.contains("substring(0, 47) + '...'"), "Should add ellipsis to truncated text");
  }

  @Test
  public void testTooltipPositioning() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("getBoundingClientRect()"),
        "Should calculate position using getBoundingClientRect");
    assertTrue(
        notebookHtml.contains("this.tooltip.style.position = 'fixed'"),
        "Tooltip should use fixed positioning");
    assertTrue(
        notebookHtml.contains("sidebarRect.right"), "Should position to the right of sidebar");
  }

  @Test
  public void testEmptyDataHandling() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("preview-empty"), "Should have empty state styling");
    assertTrue(notebookHtml.contains("No data"), "Should show 'No data' message for empty tables");
  }

  @Test
  public void testTooltipStyles() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("data-preview-styles"),
        "Should add styles with data-preview-styles id");
    assertTrue(notebookHtml.contains(".data-preview-tooltip"), "Should define tooltip styles");
  }

  @Test
  public void testAnimations() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("@keyframes preview-fade-in"), "Should have fade-in animation");
    assertTrue(
        notebookHtml.contains("animation: preview-fade-in"),
        "Tooltip should use fade-in animation");
  }

  @Test
  public void testMaxDimensions() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("max-width: 600px"), "Tooltip should have max width");
    assertTrue(
        notebookHtml.contains("max-height: 400px") || notebookHtml.contains("max-height: 280px"),
        "Tooltip/table should have max height");
  }

  @Test
  public void testStickyHeader() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("position: sticky"), "Table headers should be sticky");
    assertTrue(notebookHtml.contains("top: 0"), "Sticky headers should stick to top");
  }

  @Test
  public void testMouseAwayHandling() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("currentTable"), "Should track current table");
    assertTrue(
        notebookHtml.contains("if (this.currentTable !== tableName) return"),
        "Should cancel if mouse moved away");
  }

  @Test
  public void testErrorHandling() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Check for try-catch in show method
    int showMethodStart = notebookHtml.indexOf("show(tableName, element)");
    int nextMethodStart = notebookHtml.indexOf("hide()", showMethodStart);
    String showMethod = notebookHtml.substring(showMethodStart, nextMethodStart);

    assertTrue(
        showMethod.contains("try") && showMethod.contains("catch"),
        "show method should have error handling");
  }

  @Test
  public void testFooterHint() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("💡 Click to insert query")
            || notebookHtml.contains("Click to insert query"),
        "Footer should have hint about clicking to insert");
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
