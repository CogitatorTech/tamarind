package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for QueryResult. */
class QueryResultTest {

  @Test
  void testQueryResultCreation() {
    List<String> columns = List.of("id", "name", "age");
    List<Map<String, Object>> rows =
        List.of(
            Map.of("id", 1, "name", "Alice", "age", 30), Map.of("id", 2, "name", "Bob", "age", 25));

    QueryResult result = new QueryResult(columns, rows);

    assertNotNull(result);
    assertEquals(3, result.getColumnCount());
    assertEquals(2, result.getRowCount());
    assertEquals(columns, result.getColumnNames());
    assertEquals(rows, result.getRows());
  }

  @Test
  void testEmptyQueryResult() {
    QueryResult result = new QueryResult(List.of(), List.of());

    assertEquals(0, result.getColumnCount());
    assertEquals(0, result.getRowCount());
    assertTrue(result.getColumnNames().isEmpty());
    assertTrue(result.getRows().isEmpty());
  }

  @Test
  void testQueryResultWithNullValues() {
    List<String> columns = List.of("id", "name");
    List<Map<String, Object>> rows =
        List.of(
            Map.of("id", 1, "name", "Alice"), Map.of("id", 2) // name is missing
            );

    QueryResult result = new QueryResult(columns, rows);

    assertEquals(2, result.getColumnCount());
    assertEquals(2, result.getRowCount());
  }

  @Test
  void testSingleRowResult() {
    List<String> columns = List.of("count");
    List<Map<String, Object>> rows = List.of(Map.of("count", 42));

    QueryResult result = new QueryResult(columns, rows);

    assertEquals(1, result.getColumnCount());
    assertEquals(1, result.getRowCount());
    assertEquals(42, result.getRows().get(0).get("count"));
  }

  @Test
  void testLargeResultSet() {
    List<String> columns = List.of("id", "value");
    List<Map<String, Object>> rows = new java.util.ArrayList<>();

    for (int i = 0; i < 1000; i++) {
      rows.add(Map.of("id", i, "value", "value_" + i));
    }

    QueryResult result = new QueryResult(columns, rows);

    assertEquals(2, result.getColumnCount());
    assertEquals(1000, result.getRowCount());
  }

  @Test
  void testGetColumnNames() {
    List<String> columns = List.of("first_name", "last_name", "email");
    QueryResult result = new QueryResult(columns, List.of());

    List<String> retrievedColumns = result.getColumnNames();
    assertEquals(3, retrievedColumns.size());
    assertTrue(retrievedColumns.contains("first_name"));
    assertTrue(retrievedColumns.contains("last_name"));
    assertTrue(retrievedColumns.contains("email"));
  }

  @Test
  void testDifferentDataTypes() {
    List<String> columns = List.of("int_col", "string_col", "double_col", "bool_col");
    List<Map<String, Object>> rows =
        List.of(Map.of("int_col", 42, "string_col", "test", "double_col", 3.14, "bool_col", true));

    QueryResult result = new QueryResult(columns, rows);

    assertEquals(4, result.getColumnCount());
    assertEquals(1, result.getRowCount());

    Map<String, Object> row = result.getRows().get(0);
    assertEquals(42, row.get("int_col"));
    assertEquals("test", row.get("string_col"));
    assertEquals(3.14, row.get("double_col"));
    assertEquals(true, row.get("bool_col"));
  }
}
