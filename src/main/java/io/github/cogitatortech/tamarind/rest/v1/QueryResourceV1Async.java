package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.cache.QueryResultCache;
import io.github.cogitatortech.tamarind.config.QueryConfig;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryOptimizer;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import io.github.cogitatortech.tamarind.rest.v1.dto.QueryRequest;
import io.github.cogitatortech.tamarind.rest.v1.dto.QueryResponse;
import io.github.cogitatortech.tamarind.security.AuditLogger;
import io.github.cogitatortech.tamarind.security.RateLimiter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reactive/async version of QueryResourceV1 for high-concurrency scenarios (>1000 concurrent
 * users). Uses SmallRye Mutiny for non-blocking I/O.
 */
@Path("/api/v1/query-async")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueryResourceV1Async {

  private static final Logger LOGGER = LogManager.getLogger(QueryResourceV1Async.class);

  @Inject QueryEngine engine;
  @Inject QueryResultCache cache;
  @Inject QueryConfig config;
  @Inject RateLimiter rateLimiter;
  @Inject AuditLogger auditLogger;
  @Inject QueryOptimizer queryOptimizer;

  private static final int MAX_FIELD_STRING_LENGTH = RowSanitizer.MAX_FIELD_STRING_LENGTH;
  private static final int MAX_ARRAY_ELEMENTS = RowSanitizer.MAX_ARRAY_ELEMENTS;

  @POST
  public Uni<Response> executeQueryAsync(
      QueryRequest request, @Context SecurityContext securityContext) {
    String userId = getUserId(securityContext);
    long startTime = System.currentTimeMillis();

    // Validate request synchronously (fast operations)
    if (request == null) {
      return Uni.createFrom()
          .item(
              Response.status(Response.Status.BAD_REQUEST)
                  .entity(ApiResponse.error("INVALID_REQUEST", "Request body cannot be null"))
                  .build());
    }

    if (!rateLimiter.allowRequest(userId)) {
      auditLogger.logRateLimitViolation(userId, "/api/v1/query-async");
      return Uni.createFrom()
          .item(
              Response.status(Response.Status.TOO_MANY_REQUESTS)
                  .entity(
                      ApiResponse.error(
                          "RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."))
                  .build());
    }

    String sql = request.getSql();
    if (sql == null || sql.trim().isEmpty()) {
      auditLogger.logSecurityViolation(userId, "EMPTY_QUERY", "SQL query cannot be empty");
      return Uni.createFrom()
          .item(
              Response.status(Response.Status.BAD_REQUEST)
                  .entity(ApiResponse.error("INVALID_REQUEST", "SQL query cannot be empty"))
                  .build());
    }

    // Validate query using optimizer
    QueryOptimizer.ValidationResult validation = queryOptimizer.validate(sql);
    if (!validation.isValid()) {
      String errorMsg = String.join(", ", validation.getErrors());
      LOGGER.warn("Invalid query from user {}: {}", userId, errorMsg);
      auditLogger.logSecurityViolation(userId, "INVALID_QUERY", errorMsg);
      return Uni.createFrom()
          .item(
              Response.status(Response.Status.FORBIDDEN)
                  .entity(
                      ApiResponse.error("FORBIDDEN_OPERATION", "Query validation failed", errorMsg))
                  .build());
    }

    // Log warnings if any
    if (validation.hasWarnings()) {
      LOGGER.info("Query warnings for user {}: {}", userId, validation.getWarnings());
    }

    // Optimize query
    QueryOptimizer.OptimizationOptions opts = QueryOptimizer.OptimizationOptions.defaults();
    QueryOptimizer.OptimizationResult optimization = queryOptimizer.optimize(sql, opts);
    String optimizedSql = optimization.getOptimizedSql();

    if (!optimization.getAppliedOptimizations().isEmpty()) {
      LOGGER.debug("Applied optimizations: {}", optimization.getAppliedOptimizations());
    }

    boolean cachingEnabled = config.cachingEnabled().orElse(true);

    // Check cache first (non-blocking)
    return Uni.createFrom()
        .item(() -> cache.get(optimizedSql))
        .onItem()
        .transformToUni(
            cachedResult -> {
              if (cachedResult != null) {
                LOGGER.debug("Cache hit for query");
                return Uni.createFrom()
                    .item(cachedResult)
                    .map(
                        result ->
                            buildSuccessResponse(request, result, startTime, userId, sql, true));
              } else {
                // Execute query asynchronously on worker thread pool
                return Uni.createFrom()
                    .item(
                        () -> {
                          try {
                            LOGGER.info(
                                "Executing query for user {}: {}", userId, truncate(optimizedSql));
                            QueryResult result = engine.executeQuery(optimizedSql);

                            if (cachingEnabled) {
                              cache.put(optimizedSql, result);
                            }

                            return result;
                          } catch (SQLException e) {
                            throw new RuntimeException(e);
                          }
                        })
                    .runSubscriptionOn(
                        io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool())
                    .map(
                        result ->
                            buildSuccessResponse(request, result, startTime, userId, sql, false));
              }
            })
        .onFailure()
        .recoverWithItem(
            throwable -> {
              LOGGER.error(
                  "Error executing query for user {}: {}",
                  userId,
                  throwable.getMessage(),
                  throwable);

              if (throwable.getCause() instanceof SQLException) {
                auditLogger.logError(userId, sql, throwable.getCause().getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        ApiResponse.error(
                            "SQL_ERROR",
                            "Query execution failed",
                            throwable.getCause().getMessage()))
                    .build();
              } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        ApiResponse.error(
                            "INTERNAL_ERROR",
                            "An unexpected error occurred",
                            throwable.getMessage()))
                    .build();
              }
            });
  }

  private Response buildSuccessResponse(
      QueryRequest request,
      QueryResult result,
      long startTime,
      String userId,
      String sql,
      boolean fromCache) {

    int maxRows = getMaxRows(request);
    List<Map<String, Object>> rows = result.getRows();
    boolean truncated = false;

    if (rows.size() > maxRows) {
      LOGGER.warn("Query returned {} rows, limiting to {}", rows.size(), maxRows);
      rows = rows.subList(0, maxRows);
      truncated = true;
    }

    long executionTime = System.currentTimeMillis() - startTime;
    auditLogger.logQuery(userId, sql, executionTime, rows.size());

    RowSanitizer.Sanitized sanitizeResult = RowSanitizer.sanitize(rows);

    QueryResponse queryResponse =
        new QueryResponse(
            result.getColumnNames(),
            sanitizeResult.rows(),
            request.getOptions() != null && request.getOptions().getIncludeSchema()
                ? inferSchema(result)
                : null);

    ApiResponse.ResponseMetadata metadata =
        new ApiResponse.ResponseMetadata(executionTime, rows.size(), fromCache, truncated);

    if (sanitizeResult.truncated()) {
      Map<String, Object> additional = new HashMap<>();
      additional.put("fieldTruncation", true);
      additional.put("maxArrayElements", MAX_ARRAY_ELEMENTS);
      additional.put("maxStringLength", MAX_FIELD_STRING_LENGTH);
      metadata.setAdditional(additional);
    }

    return Response.ok(ApiResponse.success(queryResponse, metadata)).build();
  }

  private int getMaxRows(QueryRequest request) {
    if (request.getOptions() != null && request.getOptions().getMaxRows() != null) {
      return request.getOptions().getMaxRows();
    }
    return config.maxResultRows().orElse(10000);
  }

  private Map<String, String> inferSchema(QueryResult result) {
    Map<String, String> schema = new HashMap<>();
    if (!result.getRows().isEmpty()) {
      Map<String, Object> firstRow = result.getRows().get(0);
      for (String column : result.getColumnNames()) {
        Object value = firstRow.get(column);
        schema.put(column, value != null ? value.getClass().getSimpleName() : "NULL");
      }
    }
    return schema;
  }

  private String getUserId(SecurityContext securityContext) {
    if (securityContext != null && securityContext.getUserPrincipal() != null) {
      return securityContext.getUserPrincipal().getName();
    }
    return "anonymous";
  }

  private String truncate(String sql) {
    if (sql == null) {
      return null;
    }
    return sql.length() > 100 ? sql.substring(0, 97) + "..." : sql;
  }
}
