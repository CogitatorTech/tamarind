package io.github.cogitatortech.tamarind.webui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for notebook UI bug fixes
 *
 * <p>Tests verify that the undefined typeKey variable bug is fixed and that schema rendering
 * handles various column name conventions correctly.
 */
public class NotebookSchemaBugFixTest {

  @Test
  public void testNotebookSchemaFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");
    assertTrue(
        notebookHtml.contains("async function showTableSchema(table)"),
        "notebook.html should define showTableSchema function");
  }

  @Test
  public void testSchemaFunctionDefinesTypeKey() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    // Find the showTableSchema function
    int funcStart = notebookHtml.indexOf("async function showTableSchema(table)");
    int funcEnd = notebookHtml.indexOf("async function", funcStart + 1);
    if (funcEnd == -1) funcEnd = notebookHtml.length();

    String schemaFunction = notebookHtml.substring(funcStart, funcEnd);

    // Verify typeKey is defined before being used
    int typeKeyDefinition = schemaFunction.indexOf("let typeKey");
    int typeKeyUsage = schemaFunction.indexOf("row[typeKey]");

    assertTrue(typeKeyDefinition > 0, "typeKey should be defined in showTableSchema");
    assertTrue(typeKeyUsage > 0, "typeKey should be used in showTableSchema");
    assertTrue(typeKeyDefinition < typeKeyUsage, "typeKey should be defined before it's used");
  }

  @Test
  public void testSchemaHandlesColumnNameVariations() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    // Verify the function checks for multiple column name conventions
    assertTrue(
        notebookHtml.contains("lowerCols.indexOf('column_name')"),
        "Should check for 'column_name' convention");
    assertTrue(
        notebookHtml.contains("lowerCols.indexOf('name')"), "Should check for 'name' convention");
    assertTrue(
        notebookHtml.contains("lowerCols.indexOf('column_type')"),
        "Should check for 'column_type' convention");
    assertTrue(
        notebookHtml.contains("lowerCols.indexOf('type')"), "Should check for 'type' convention");
  }

  @Test
  public void testSchemaHasFallbackForColumnNames() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    int funcStart = notebookHtml.indexOf("async function showTableSchema(table)");
    int funcEnd = notebookHtml.indexOf("async function", funcStart + 1);
    if (funcEnd == -1) funcEnd = notebookHtml.length();

    String schemaFunction = notebookHtml.substring(funcStart, funcEnd);

    // Verify fallback to first and second columns
    assertTrue(
        schemaFunction.contains("columns[0]"), "Should have fallback to first column for name");
    assertTrue(
        schemaFunction.contains("columns[1]"), "Should have fallback to second column for type");
  }

  @Test
  public void testSchemaDefinesTypeKeyTwice() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    int funcStart = notebookHtml.indexOf("async function showTableSchema(table)");
    int funcEnd = notebookHtml.indexOf("async function", funcStart + 1);
    if (funcEnd == -1) funcEnd = notebookHtml.length();

    String schemaFunction = notebookHtml.substring(funcStart, funcEnd);

    // Count occurrences of "let typeKey" - should be defined in both the hint caching
    // section and the rendering section
    int count = countOccurrences(schemaFunction, "let typeKey");
    assertTrue(
        count >= 2,
        "typeKey should be defined in both hint caching and rendering sections (found "
            + count
            + ")");
  }

  @Test
  public void testSchemaUsesEscapeHtml() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    int funcStart = notebookHtml.indexOf("async function showTableSchema(table)");
    int funcEnd = notebookHtml.indexOf("async function", funcStart + 1);
    if (funcEnd == -1) funcEnd = notebookHtml.length();

    String schemaFunction = notebookHtml.substring(funcStart, funcEnd);

    // Verify HTML escaping is used for safety
    assertTrue(
        schemaFunction.contains("escapeHtml(String(name))"), "Column names should be HTML escaped");
    assertTrue(
        schemaFunction.contains("escapeHtml(String(type))"), "Column types should be HTML escaped");
  }

  @Test
  public void testEscapeHtmlFunctionExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");
    assertTrue(
        notebookHtml.contains("function escapeHtml(text)"),
        "escapeHtml function should be defined");
  }

  @Test
  public void testSchemaModalExists() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");
    assertTrue(notebookHtml.contains("id=\"schemaModal\""), "Schema modal should exist in HTML");
    assertTrue(
        notebookHtml.contains("id=\"schemaContent\""), "Schema content container should exist");
  }

  @Test
  public void testSchemaHandlesErrors() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    int funcStart = notebookHtml.indexOf("async function showTableSchema(table)");
    int funcEnd = notebookHtml.indexOf("async function", funcStart + 1);
    if (funcEnd == -1) funcEnd = notebookHtml.length();

    String schemaFunction = notebookHtml.substring(funcStart, funcEnd);

    // Verify error handling
    assertTrue(
        schemaFunction.contains("try {") && schemaFunction.contains("} catch"),
        "Schema function should have error handling");
    assertTrue(
        schemaFunction.contains("Error loading schema")
            || schemaFunction.contains("Failed to load schema"),
        "Schema function should show error message on failure");
  }

  @Test
  public void testSchemaCallsDescribeQuery() {
    String notebookHtml = readResourceFile("/META-INF/resources/notebook.html");

    assertNotNull(notebookHtml, "notebook.html should be readable");

    int funcStart = notebookHtml.indexOf("async function showTableSchema(table)");
    int funcEnd = notebookHtml.indexOf("async function", funcStart + 1);
    if (funcEnd == -1) funcEnd = notebookHtml.length();

    String schemaFunction = notebookHtml.substring(funcStart, funcEnd);

    // Verify it executes a DESCRIBE query
    assertTrue(
        schemaFunction.contains("DESCRIBE"), "Schema function should execute DESCRIBE query");
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
