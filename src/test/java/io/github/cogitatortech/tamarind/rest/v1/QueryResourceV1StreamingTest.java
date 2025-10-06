package io.github.cogitatortech.tamarind.rest.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.cache.QueryResultCache;
import io.github.cogitatortech.tamarind.config.QueryConfig;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import io.github.cogitatortech.tamarind.rest.v1.dto.QueryRequest;
import io.github.cogitatortech.tamarind.security.AuditLogger;
import io.github.cogitatortech.tamarind.security.RateLimiter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class QueryResourceV1StreamingTest {

  private QueryResourceV1 resource;

  @Mock private QueryEngine engine;
  @Mock private QueryResultCache cache;
  @Mock private QueryConfig config;
  @Mock private RateLimiter rateLimiter;
  @Mock private AuditLogger auditLogger;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resource = new QueryResourceV1();
    resource.engine = engine;
    resource.cache = cache;
    resource.config = config;
    resource.rateLimiter = rateLimiter;
    resource.auditLogger = auditLogger;
  }

  @Test
  void streamQueryOutputsCsv() throws Exception {
    QueryRequest req = new QueryRequest();
    req.setSql("SELECT 1 as id, 'Alice' as name");

    QueryResult result =
        new QueryResult(
            List.of("id", "name"),
            List.of(Map.of("id", 1, "name", "Alice"), Map.of("id", 2, "name", "Bob")));

    when(engine.executeQuery(anyString())).thenReturn(result);

    Response resp = resource.streamQuery(req, null);
    assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof StreamingOutput);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ((StreamingOutput) entity).write(baos);

    String csv = baos.toString(StandardCharsets.UTF_8);
    String[] lines = csv.split("\n");
    assertEquals("id,name", lines[0]);
    assertEquals("1,Alice", lines[1]);
    assertEquals("2,Bob", lines[2]);
  }
}
