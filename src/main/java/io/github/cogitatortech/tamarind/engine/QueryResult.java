package io.github.cogitatortech.tamarind.engine;

import java.util.List;
import java.util.Map;

/** Represents the result of a query execution. */
public class QueryResult {
  private final List<String> columnNames;
  private final List<Map<String, Object>> rows;

  public QueryResult(List<String> columnNames, List<Map<String, Object>> rows) {
    this.columnNames = columnNames;
    this.rows = rows;
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public int getRowCount() {
    return rows.size();
  }

  public int getColumnCount() {
    return columnNames.size();
  }
}
