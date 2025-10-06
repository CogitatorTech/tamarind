package io.github.cogitatortech.tamarind.rest.v1.dto;

import java.util.List;
import java.util.Map;

/** Response object containing query execution results. */
public class QueryResponse {
  private List<String> columns;
  private List<Map<String, Object>> rows;
  private Map<String, String> schema;

  public QueryResponse() {}

  public QueryResponse(
      List<String> columns, List<Map<String, Object>> rows, Map<String, String> schema) {
    this.columns = columns;
    this.rows = rows;
    this.schema = schema;
  }

  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public void setRows(List<Map<String, Object>> rows) {
    this.rows = rows;
  }

  public Map<String, String> getSchema() {
    return schema;
  }

  public void setSchema(Map<String, String> schema) {
    this.schema = schema;
  }
}
