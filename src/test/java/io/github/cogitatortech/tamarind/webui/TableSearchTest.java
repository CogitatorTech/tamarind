package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for Table Search feature
 *
 * <p>Verifies that tables can be filtered by name using the search input in the sidebar.
 */
public class TableSearchTest {

  @Test
  public void testSearchInputExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("id=\"tableSearch\""), "Should have table search input");
    assertTrue(notebookHtml.contains("type=\"search\""), "Search input should be of type search");
  }

  @Test
  public void testSearchInputPlaceholder() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("placeholder=\"Search tables...\""),
        "Search input should have placeholder text");
  }

  @Test
  public void testFilterTablesFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function filterTables(query)"), "Should have filterTables function");
  }

  @Test
  public void testSearchInputCallsFilterTables() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("oninput=\"filterTables(this.value)\""),
        "Search input should call filterTables on input");
  }

  @Test
  public void testFilteringLogic() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    // Find filterTables function
    int funcStart = notebookHtml.indexOf("function filterTables(query)");
    int funcEnd = notebookHtml.indexOf("function escapeRegex", funcStart);
    String filterFunction = notebookHtml.substring(funcStart, funcEnd);

    assertTrue(filterFunction.contains("toLowerCase()"), "Should be case-insensitive");
    assertTrue(
        filterFunction.contains("includes(lowerQuery)"),
        "Should check if table name includes query");
  }

  @Test
  public void testHideNonMatchingTables() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("classList.add('hidden')"), "Should hide non-matching tables");
    assertTrue(notebookHtml.contains("classList.remove('hidden')"), "Should show matching tables");
  }

  @Test
  public void testHighlightMatchingTables() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("classList.add('highlight')"), "Should highlight matching tables");
    assertTrue(
        notebookHtml.contains("classList.remove('highlight')"),
        "Should remove highlight from non-matching tables");
  }

  @Test
  public void testTextHighlighting() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("<mark>"), "Should use <mark> tag for highlighting");
    assertTrue(
        notebookHtml.contains("replace(regex"), "Should use regex replacement for highlighting");
  }

  @Test
  public void testEmptyQueryShowsAllTables() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    int funcStart = notebookHtml.indexOf("function filterTables(query)");
    int funcEnd = notebookHtml.indexOf("function escapeRegex", funcStart);
    String filterFunction = notebookHtml.substring(funcStart, funcEnd);

    assertTrue(filterFunction.contains("if (!lowerQuery)"), "Should check for empty query");
    assertTrue(filterFunction.contains("return"), "Should return early for empty query");
  }

  @Test
  public void testSearchInfoDisplay() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("id=\"tableSearchInfo\""), "Should have search info element");
  }

  @Test
  public void testNoResultsMessage() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("No tables match"), "Should show 'no tables match' message");
    assertTrue(notebookHtml.contains("no-results"), "Should have no-results class for styling");
  }

  @Test
  public void testMatchCountDisplay() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("Showing ${matchCount} of ${tableItems.length}"),
        "Should show match count");
  }

  @Test
  public void testEscapeRegexFunction() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("function escapeRegex(string)"), "Should have escapeRegex function");
    assertTrue(
        notebookHtml.contains("replace(/[.*+?^${}()|[\\]\\\\]/g"),
        "Should escape special regex characters");
  }

  @Test
  public void testSearchWrapperStyles() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains(".table-search-wrapper"), "Should have search wrapper styles");
    assertTrue(notebookHtml.contains(".table-search-input"), "Should have search input styles");
  }

  @Test
  public void testSearchIconDisplay() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains("table-search-icon"), "Should have search icon element");
    assertTrue(notebookHtml.contains("🔍"), "Should display search icon emoji");
  }

  @Test
  public void testHiddenTableStyle() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains(".table-item.hidden"), "Should have hidden table style");
    assertTrue(notebookHtml.contains("display: none"), "Hidden tables should not be displayed");
  }

  @Test
  public void testHighlightedTableStyle() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(notebookHtml.contains(".table-item.highlight"), "Should have highlight table style");
  }

  @Test
  public void testMarkTagStyle() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains(".table-item mark"), "Should have mark tag styles for highlighting");
  }

  @Test
  public void testSearchInputFocusStyles() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains(".table-search-input:focus"),
        "Should have focus styles for search input");
    assertTrue(notebookHtml.contains("box-shadow"), "Focus should include box shadow");
  }

  @Test
  public void testAccessibility() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");
    assertTrue(
        notebookHtml.contains("aria-label=\"Search tables\""),
        "Search input should have aria-label");
  }

  @Test
  public void testCaseInsensitiveSearch() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should exist");

    int funcStart = notebookHtml.indexOf("function filterTables(query)");
    int funcEnd = notebookHtml.indexOf("function escapeRegex", funcStart);
    String filterFunction = notebookHtml.substring(funcStart, funcEnd);

    int lowerQueryCount = 0;
    int index = 0;
    while ((index = filterFunction.indexOf("toLowerCase()", index)) != -1) {
      lowerQueryCount++;
      index += "toLowerCase()".length();
    }

    assertTrue(lowerQueryCount >= 2, "Should convert both query and table names to lowercase");
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
