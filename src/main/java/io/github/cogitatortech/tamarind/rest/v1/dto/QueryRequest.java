package io.github.cogitatortech.tamarind.rest.v1.dto;

import java.util.List;

/** Request object for executing SQL queries via the v1 API. */
public class QueryRequest {
  private String sql;
  private List<Object> parameters;
  private QueryOptions options;

  public QueryRequest() {}

  public QueryRequest(String sql) {
    this.sql = sql;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public List<Object> getParameters() {
    return parameters;
  }

  public void setParameters(List<Object> parameters) {
    this.parameters = parameters;
  }

  public QueryOptions getOptions() {
    return options;
  }

  public void setOptions(QueryOptions options) {
    this.options = options;
  }

  public static class QueryOptions {
    private Integer maxRows;
    private Integer timeout;
    private String format = "json";
    private Boolean includeSchema = false;

    public Integer getMaxRows() {
      return maxRows;
    }

    public void setMaxRows(Integer maxRows) {
      this.maxRows = maxRows;
    }

    public Integer getTimeout() {
      return timeout;
    }

    public void setTimeout(Integer timeout) {
      this.timeout = timeout;
    }

    public String getFormat() {
      return format;
    }

    public void setFormat(String format) {
      this.format = format;
    }

    public Boolean getIncludeSchema() {
      return includeSchema;
    }

    public void setIncludeSchema(Boolean includeSchema) {
      this.includeSchema = includeSchema;
    }
  }
}
