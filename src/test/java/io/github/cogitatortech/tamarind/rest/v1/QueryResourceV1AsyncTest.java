package io.github.cogitatortech.tamarind.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.github.cogitatortech.tamarind.cache.QueryResultCache;
import io.github.cogitatortech.tamarind.config.QueryConfig;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryOptimizer;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import io.github.cogitatortech.tamarind.rest.v1.dto.QueryRequest;
import io.github.cogitatortech.tamarind.security.AuditLogger;
import io.github.cogitatortech.tamarind.security.RateLimiter;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for QueryResourceV1Async (reactive endpoint). */
class QueryResourceV1AsyncTest {

  private QueryResourceV1Async resource;

  @Mock private QueryEngine engine;
  @Mock private QueryResultCache cache;
  @Mock private QueryConfig config;
  @Mock private RateLimiter rateLimiter;
  @Mock private AuditLogger auditLogger;
  @Mock private QueryOptimizer queryOptimizer;
  @Mock private SecurityContext securityContext;
  @Mock private Principal principal;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resource = new QueryResourceV1Async();
    resource.engine = engine;
    resource.cache = cache;
    resource.config = config;
    resource.rateLimiter = rateLimiter;
    resource.auditLogger = auditLogger;
    resource.queryOptimizer = queryOptimizer;

    when(securityContext.getUserPrincipal()).thenReturn(principal);
    when(principal.getName()).thenReturn("testUser");
    when(rateLimiter.allowRequest(anyString())).thenReturn(true);

    // Mock QueryOptimizer to return valid results by default
    QueryOptimizer.ValidationResult validationResult =
        new QueryOptimizer.ValidationResult(true, new ArrayList<>(), new ArrayList<>());
    when(queryOptimizer.validate(anyString())).thenReturn(validationResult);

    QueryOptimizer.OptimizationResult optimizationResult =
        new QueryOptimizer.OptimizationResult(
            "SELECT * FROM test",
            new ArrayList<>(),
            new QueryOptimizer.QueryCost(10, QueryOptimizer.QueryCost.Complexity.SIMPLE));
    when(queryOptimizer.optimize(anyString(), any())).thenReturn(optimizationResult);
  }

  @Test
  void testExecuteQueryAsyncSuccess() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM test");

    QueryResult result =
        new QueryResult(List.of("id", "name"), List.of(Map.of("id", 1, "name", "Alice")));

    when(cache.get(anyString())).thenReturn(null);
    when(engine.executeQuery(anyString())).thenReturn(result);

    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(request, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(engine).executeQuery(anyString());
  }

  @Test
  void testExecuteQueryAsyncWithNullRequest() {
    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(null, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryAsyncWithEmptySql() {
    QueryRequest request = new QueryRequest();
    request.setSql("");

    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(request, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryAsyncRateLimitExceeded() {
    when(rateLimiter.allowRequest(anyString())).thenReturn(false);

    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM test");

    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(request, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
    verify(auditLogger).logRateLimitViolation(anyString(), anyString());
  }

  @Test
  void testExecuteQueryAsyncWithSQLException() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM nonexistent");

    when(cache.get(anyString())).thenReturn(null);
    when(engine.executeQuery(anyString())).thenThrow(new SQLException("Table not found"));

    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(request, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryAsyncUsesCache() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM test");

    QueryResult cachedResult = new QueryResult(List.of("id"), List.of(Map.of("id", 1)));
    when(cache.get(anyString())).thenReturn(cachedResult);

    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(request, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(engine, never()).executeQuery(anyString());
  }

  @Test
  void testExecuteQueryAsyncInvalidQuery() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("DROP TABLE users");

    // Mock validation to return invalid
    QueryOptimizer.ValidationResult invalidResult =
        new QueryOptimizer.ValidationResult(
            false, List.of("Dangerous operation detected"), new ArrayList<>());
    when(queryOptimizer.validate(anyString())).thenReturn(invalidResult);

    UniAssertSubscriber<Response> subscriber =
        resource
            .executeQueryAsync(request, securityContext)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    Response response = subscriber.awaitItem().getItem();

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    verify(engine, never()).executeQuery(anyString());
  }
}
