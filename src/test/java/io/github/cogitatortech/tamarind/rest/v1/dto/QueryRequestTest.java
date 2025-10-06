package io.github.cogitatortech.tamarind.rest.v1.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for QueryRequest. */
class QueryRequestTest {

  @Test
  void testQueryRequestCreation() {
    QueryRequest request = new QueryRequest();

    assertNotNull(request);
    assertNull(request.getSql());
    assertNull(request.getParameters());
    assertNull(request.getOptions());
  }

  @Test
  void testSetAndGetSql() {
    QueryRequest request = new QueryRequest();
    String sql = "SELECT * FROM test";

    request.setSql(sql);

    assertEquals(sql, request.getSql());
  }

  @Test
  void testConstructorWithSql() {
    String sql = "SELECT * FROM users";
    QueryRequest request = new QueryRequest(sql);

    assertEquals(sql, request.getSql());
  }

  @Test
  void testSetAndGetParameters() {
    QueryRequest request = new QueryRequest();
    List<Object> params = List.of(1, "test", true);

    request.setParameters(params);

    assertEquals(params, request.getParameters());
  }

  @Test
  void testSetAndGetOptions() {
    QueryRequest request = new QueryRequest();
    QueryRequest.QueryOptions options = new QueryRequest.QueryOptions();
    options.setMaxRows(100);

    request.setOptions(options);

    assertNotNull(request.getOptions());
    assertEquals(100, request.getOptions().getMaxRows());
  }

  @Test
  void testQueryOptionsDefaults() {
    QueryRequest.QueryOptions options = new QueryRequest.QueryOptions();

    assertEquals("json", options.getFormat());
    assertEquals(false, options.getIncludeSchema());
    assertNull(options.getMaxRows());
    assertNull(options.getTimeout());
  }

  @Test
  void testQueryOptionsSetters() {
    QueryRequest.QueryOptions options = new QueryRequest.QueryOptions();

    options.setMaxRows(50);
    options.setTimeout(30);
    options.setFormat("csv");
    options.setIncludeSchema(true);

    assertEquals(50, options.getMaxRows());
    assertEquals(30, options.getTimeout());
    assertEquals("csv", options.getFormat());
    assertTrue(options.getIncludeSchema());
  }

  @Test
  void testCompleteQueryRequest() {
    QueryRequest request = new QueryRequest("SELECT * FROM users WHERE id = ?");
    request.setParameters(List.of(1));

    QueryRequest.QueryOptions options = new QueryRequest.QueryOptions();
    options.setMaxRows(100);
    options.setTimeout(60);
    request.setOptions(options);

    assertEquals("SELECT * FROM users WHERE id = ?", request.getSql());
    assertEquals(1, request.getParameters().size());
    assertEquals(100, request.getOptions().getMaxRows());
  }
}
