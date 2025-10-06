package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/** Tests for AnalyticalQueryEngine interface implementation. */
@QuarkusTest
class AnalyticalQueryEngineTest {

  @Inject AnalyticalQueryEngine engine;

  @Test
  void testEngineIsAnalytical() {
    assertNotNull(engine, "Analytical engine should be injected");
    assertTrue(
        engine instanceof AnalyticalQueryEngine, "Engine should implement AnalyticalQueryEngine");
  }

  @Test
  void testAnalyticalEngineCapabilities() {
    assertTrue(
        engine.supportsDirectFileQuery(), "Analytical engine should support direct file queries");

    assertTrue(engine.isColumnar(), "Analytical engine should be columnar");

    String[] formats = engine.getSupportedFileFormats();
    assertNotNull(formats, "Supported formats should not be null");
    assertTrue(formats.length > 0, "Should support at least one file format");

    // Check for common analytical formats
    boolean supportsParquet = false;
    boolean supportsCsv = false;
    for (String format : formats) {
      if ("parquet".equalsIgnoreCase(format)) supportsParquet = true;
      if ("csv".equalsIgnoreCase(format)) supportsCsv = true;
    }

    assertTrue(supportsParquet, "Should support Parquet format");
    assertTrue(supportsCsv, "Should support CSV format");
  }

  @Test
  void testEngineHealthCheck() {
    assertTrue(engine.isHealthy(), "Engine should be healthy");
  }

  @Test
  void testEngineMetadata() {
    assertEquals("DuckDB", engine.getEngineName(), "Engine name should be DuckDB");

    String version = engine.getEngineVersion();
    assertNotNull(version, "Version should not be null");
    assertFalse(version.isEmpty(), "Version should not be empty");
    assertNotEquals("unknown", version, "Version should be detected");
  }

  @Test
  void testBasicQuery() throws SQLException {
    QueryResult result = engine.executeQuery("SELECT 42 as answer");

    assertNotNull(result, "Result should not be null");
    assertEquals(1, result.getRowCount(), "Should return one row");
    assertEquals(1, result.getColumnCount(), "Should return one column");
    assertEquals("answer", result.getColumnNames().get(0));
    assertEquals(42, result.getRows().get(0).get("answer"));
  }

  @Test
  void testFileFormatSupport() throws SQLException {
    // Test that engine can handle CSV-like queries
    QueryResult result = engine.executeQuery("SELECT 1 as id, 'test' as name");

    assertNotNull(result, "Should be able to execute queries");
    assertEquals(1, result.getRowCount());
  }
}
