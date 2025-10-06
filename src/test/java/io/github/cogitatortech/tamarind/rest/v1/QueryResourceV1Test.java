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
import io.github.cogitatortech.tamarind.security.UserService;
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

/** Unit tests for QueryResourceV1. */
class QueryResourceV1Test {

  private static final String VALID_TOKEN = "Bearer valid-token-123";
  private QueryResourceV1 resource;
  @Mock private QueryEngine engine;
  @Mock private QueryResultCache cache;
  @Mock private QueryConfig config;
  @Mock private RateLimiter rateLimiter;
  @Mock private AuditLogger auditLogger;
  @Mock private QueryOptimizer queryOptimizer;
  @Mock private UserService userService;
  @Mock private SecurityContext securityContext;
  @Mock private Principal principal;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resource = new QueryResourceV1();
    resource.engine = engine;
    resource.cache = cache;
    resource.config = config;
    resource.rateLimiter = rateLimiter;
    resource.auditLogger = auditLogger;
    resource.queryOptimizer = queryOptimizer;
    resource.userService = userService;

    when(securityContext.getUserPrincipal()).thenReturn(principal);
    when(principal.getName()).thenReturn("testUser");
    when(rateLimiter.allowRequest(anyString())).thenReturn(true);
    when(userService.validateToken(anyString())).thenReturn(true);

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
  void testExecuteQuerySuccess() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM test");

    QueryResult result =
        new QueryResult(List.of("id", "name"), List.of(Map.of("id", 1, "name", "Alice")));

    when(cache.get(anyString())).thenReturn(null);
    when(engine.executeQuery(anyString())).thenReturn(result);

    Response response = resource.executeQuery(request, securityContext);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(engine).executeQuery(anyString());
  }

  @Test
  void testExecuteQueryWithNullRequest() {
    Response response = resource.executeQuery(null, securityContext);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryWithNullSql() {
    QueryRequest request = new QueryRequest();
    request.setSql(null);

    Response response = resource.executeQuery(request, securityContext);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryWithEmptySql() {
    QueryRequest request = new QueryRequest();
    request.setSql("");

    Response response = resource.executeQuery(request, securityContext);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryRateLimitExceeded() {
    when(rateLimiter.allowRequest(anyString())).thenReturn(false);

    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM test");

    Response response = resource.executeQuery(request, securityContext);

    assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
    verify(auditLogger).logRateLimitViolation(anyString(), anyString());
  }

  @Test
  void testExecuteQueryWithSQLException() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM nonexistent");

    when(cache.get(anyString())).thenReturn(null);
    when(engine.executeQuery(anyString())).thenThrow(new SQLException("Table not found"));

    Response response = resource.executeQuery(request, securityContext);

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testExecuteQueryUsesCache() throws SQLException {
    QueryRequest request = new QueryRequest();
    request.setSql("SELECT * FROM test");

    QueryResult cachedResult = new QueryResult(List.of("id"), List.of(Map.of("id", 1)));
    when(cache.get(anyString())).thenReturn(cachedResult);

    Response response = resource.executeQuery(request, securityContext);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(engine, never()).executeQuery(anyString());
  }
}
