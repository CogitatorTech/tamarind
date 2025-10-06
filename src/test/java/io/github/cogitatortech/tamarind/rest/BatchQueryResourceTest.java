package io.github.cogitatortech.tamarind.rest;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.engine.DuckDBEngine;
import io.github.cogitatortech.tamarind.security.AuditLogger;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BatchQueryResourceTest {

  private BatchQueryResource resource;
  private DuckDBEngine mockEngine;
  private AuditLogger mockAuditLogger;

  @BeforeEach
  void setUp() {
    resource = new BatchQueryResource();
    mockEngine = Mockito.mock(DuckDBEngine.class);
    mockAuditLogger = Mockito.mock(AuditLogger.class);

    // Use reflection to inject mocks (in a real app, use CDI)
    try {
      var engineField = BatchQueryResource.class.getDeclaredField("engine");
      engineField.setAccessible(true);
      engineField.set(resource, mockEngine);

      var auditField = BatchQueryResource.class.getDeclaredField("auditLogger");
      auditField.setAccessible(true);
      auditField.set(resource, mockAuditLogger);
    } catch (Exception e) {
      fail("Failed to inject mocks: " + e.getMessage());
    }
  }

  @Test
  void testBatchQueryRequest() {
    BatchQueryResource.BatchQueryRequest request = new BatchQueryResource.BatchQueryRequest();
    request.queries = Arrays.asList("SELECT 1", "SELECT 2", "SELECT 3");

    assertNotNull(request.queries);
    assertEquals(3, request.queries.size());
  }

  @Test
  void testBatchQueryResponse() {
    BatchQueryResource.BatchQueryResponse response = new BatchQueryResource.BatchQueryResponse();
    response.results = List.of();
    response.totalExecutionTimeMs = 100L;

    assertNotNull(response.results);
    assertEquals(100L, response.totalExecutionTimeMs);
  }

  @Test
  void testQueryResultResponse() {
    BatchQueryResource.QueryResultResponse response = new BatchQueryResource.QueryResultResponse();
    response.queryIndex = 0;
    response.sql = "SELECT 1";
    response.success = true;
    response.executionTimeMs = 50L;

    assertEquals(0, response.queryIndex);
    assertEquals("SELECT 1", response.sql);
    assertTrue(response.success);
    assertEquals(50L, response.executionTimeMs);
  }
}
