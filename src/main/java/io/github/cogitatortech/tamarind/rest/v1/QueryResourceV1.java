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
import io.github.cogitatortech.tamarind.service.QueryExecutionManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/api/v1/query")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueryResourceV1 {

  private static final Logger LOGGER = LogManager.getLogger(QueryResourceV1.class);
  private static final int MAX_FIELD_STRING_LENGTH = 1000; // characters
  private static final int MAX_ARRAY_ELEMENTS = 64; // elements per array/list
  @Inject QueryEngine engine;
  @Inject QueryResultCache cache;
  @Inject QueryConfig config;
  @Inject RateLimiter rateLimiter;
  @Inject AuditLogger auditLogger;
  @Inject QueryOptimizer queryOptimizer;
  @Inject QueryExecutionManager execManager;
  @Inject io.github.cogitatortech.tamarind.security.UserService userService;

  @POST
  public Response executeQuery(QueryRequest request, @Context SecurityContext securityContext) {

    String userId = getUserId(securityContext);
    long startTime = System.currentTimeMillis();

    try {
      if (request == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Request body cannot be null"))
            .build();
      }

      if (!rateLimiter.allowRequest(userId)) {
        auditLogger.logRateLimitViolation(userId, "/api/v1/query");
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
            .entity(
                ApiResponse.error(
                    "RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."))
            .build();
      }

      String sql = request.getSql();
      if (sql == null || sql.trim().isEmpty()) {
        auditLogger.logSecurityViolation(userId, "EMPTY_QUERY", "SQL query cannot be empty");
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "SQL query cannot be empty"))
            .build();
      }

      // Validate query using optimizer
      QueryOptimizer.ValidationResult validation = queryOptimizer.validate(sql);
      if (!validation.isValid()) {
        String errorMsg = String.join(", ", validation.getErrors());
        LOGGER.warn("Invalid query from user {}: {}", userId, errorMsg);
        auditLogger.logSecurityViolation(userId, "INVALID_QUERY", errorMsg);
        return Response.status(Response.Status.FORBIDDEN)
            .entity(ApiResponse.error("FORBIDDEN_OPERATION", "Query validation failed", errorMsg))
            .build();
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
      boolean fromCache = false;
      QueryResult result;

      if (cachingEnabled && (result = cache.get(optimizedSql)) != null) {
        LOGGER.debug("Cache hit for query");
        fromCache = true;
      } else {
        LOGGER.info("Executing query for user {}: {}", userId, truncate(optimizedSql));
        String queryId = UUID.randomUUID().toString();
        if (execManager != null) {
          execManager.track(queryId, Thread.currentThread());
        }
        try {
          result = engine.executeQuery(optimizedSql);
        } finally {
          if (execManager != null) {
            execManager.untrack(queryId);
          }
        }

        if (cachingEnabled) {
          cache.put(optimizedSql, result);
        }
      }

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

      // Sanitize fields to keep JSON safe/parseable and compact
      SanitizeResult sanitizeResult = sanitizeRows(rows);

      QueryResponse queryResponse =
          new QueryResponse(
              result.getColumnNames(),
              sanitizeResult.rows(),
              request.getOptions() != null && request.getOptions().getIncludeSchema()
                  ? inferSchema(result)
                  : null);

      ApiResponse.ResponseMetadata metadata =
          new ApiResponse.ResponseMetadata(executionTime, rows.size(), fromCache, truncated);
      if (sanitizeResult.anyTruncated()) {
        Map<String, Object> additional = new HashMap<>();
        additional.put("fieldTruncation", true);
        additional.put("maxArrayElements", MAX_ARRAY_ELEMENTS);
        additional.put("maxStringLength", MAX_FIELD_STRING_LENGTH);
        metadata.setAdditional(additional);
      }

      return Response.ok(ApiResponse.success(queryResponse, metadata)).build();

    } catch (SQLException e) {
      LOGGER.error("SQL error for user {}: {}", userId, e.getMessage(), e);
      auditLogger.logError(userId, request.getSql(), e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("SQL_ERROR", "Query execution failed", e.getMessage()))
          .build();
    } catch (Exception e) {
      LOGGER.error("Unexpected error for user {}: {}", userId, e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred", e.getMessage()))
          .build();
    }
  }

  @POST
  @Path("/stream")
  @Produces(MediaType.TEXT_PLAIN)
  public Response streamQuery(QueryRequest request, @Context SecurityContext securityContext) {
    String userId = getUserId(securityContext);

    if (request == null || request.getSql() == null || request.getSql().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("SQL must be provided").build();
    }

    StreamingOutput stream =
        (OutputStream os) -> {
          try {
            QueryResult result = engine.executeQuery(request.getSql());
            // Write CSV-like output with header
            String header = String.join(",", result.getColumnNames()) + "\n";
            os.write(header.getBytes(StandardCharsets.UTF_8));
            for (Map<String, Object> row : result.getRows()) {
              StringBuilder line = new StringBuilder();
              for (int i = 0; i < result.getColumnNames().size(); i++) {
                String col = result.getColumnNames().get(i);
                Object val = row.get(col);
                if (i > 0) line.append(',');
                // Properly escape CSV values according to RFC 4180
                line.append(escapeCsvValue(val));
              }
              line.append('\n');
              os.write(line.toString().getBytes(StandardCharsets.UTF_8));
              os.flush();
            }
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        };

    return Response.ok(stream).build();
  }

  @POST
  @Path("/cancel/{id}")
  public Response cancel(@PathParam("id") String id) {
    if (execManager == null) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
          .entity("Cancellation manager unavailable")
          .build();
    }
    boolean cancelled = execManager.cancelQuery(id);
    if (cancelled) return Response.accepted().entity("Cancelled").build();
    return Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
  }

  private int getMaxRows(QueryRequest request) {
    if (request.getOptions() != null && request.getOptions().getMaxRows() != null) {
      return request.getOptions().getMaxRows();
    }
    return config.maxResultRows().orElse(10000);
  }

  private Map<String, String> inferSchema(QueryResult result) {
    Map<String, String> schema = new HashMap<>();
    if (result == null || result.getRows() == null || result.getRows().isEmpty()) {
      return schema;
    }

    Map<String, Object> firstRow = result.getRows().get(0);
    if (firstRow == null) {
      return schema;
    }

    for (String column : result.getColumnNames()) {
      Object value = firstRow.get(column);
      schema.put(column, value != null ? value.getClass().getSimpleName() : "NULL");
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

  /**
   * Escape a value for CSV output according to RFC 4180. Values containing comma, quote, or newline
   * are wrapped in quotes. Quotes within values are escaped by doubling them.
   *
   * @param value the value to escape
   * @return properly escaped CSV value
   */
  private String escapeCsvValue(Object value) {
    if (value == null) {
      return "";
    }

    String str = value.toString();

    // Check if escaping is needed (contains comma, quote, newline, or carriage return)
    boolean needsEscaping =
        str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r");

    if (needsEscaping) {
      // Escape quotes by doubling them and wrap in quotes
      String escaped = str.replace("\"", "\"\"");
      return "\"" + escaped + "\"";
    }

    return str;
  }

  private SanitizeResult sanitizeRows(List<Map<String, Object>> original) {
    boolean[] truncatedFlag = new boolean[] {false};
    List<Map<String, Object>> sanitized =
        original.stream().map(row -> sanitizeRow(row, truncatedFlag)).toList();
    return new SanitizeResult(sanitized, truncatedFlag[0]);
  }

  private Map<String, Object> sanitizeRow(Map<String, Object> row, boolean[] truncatedFlag) {
    Map<String, Object> out = new HashMap<>();
    for (Map.Entry<String, Object> e : row.entrySet()) {
      out.put(e.getKey(), sanitizeValue(e.getValue(), truncatedFlag));
    }
    return out;
  }

  private Object sanitizeValue(Object value, boolean[] truncatedFlag) {
    if (value == null) return null;

    // Basic number handling: quote non-finite values as strings to preserve validity
    if (value instanceof Double d) {
      if (d.isNaN() || d.isInfinite()) return String.valueOf(d);
      return d;
    }
    if (value instanceof Float f) {
      if (f.isNaN() || f.isInfinite()) return String.valueOf(f);
      return f;
    }

    // Strings: truncate very long strings
    if (value instanceof CharSequence cs) {
      String s = cs.toString();
      if (s.length() > MAX_FIELD_STRING_LENGTH) {
        truncatedFlag[0] = true;
        return s.substring(0, MAX_FIELD_STRING_LENGTH) + "…";
      }
      return s;
    }

    // Primitive/object arrays
    if (value.getClass().isArray()) {
      int len = Array.getLength(value);
      List<Object> list = new java.util.ArrayList<>(Math.min(len, MAX_ARRAY_ELEMENTS));
      int limit = Math.min(len, MAX_ARRAY_ELEMENTS);
      for (int i = 0; i < limit; i++) {
        Object elem = Array.get(value, i);
        list.add(sanitizeValue(elem, truncatedFlag));
      }
      if (len > MAX_ARRAY_ELEMENTS) truncatedFlag[0] = true;
      return list;
    }

    // Lists
    if (value instanceof List<?> l) {
      int len = l.size();
      List<Object> out = new java.util.ArrayList<>(Math.min(len, MAX_ARRAY_ELEMENTS));
      int limit = Math.min(len, MAX_ARRAY_ELEMENTS);
      for (int i = 0; i < limit; i++) {
        out.add(sanitizeValue(l.get(i), truncatedFlag));
      }
      if (len > MAX_ARRAY_ELEMENTS) truncatedFlag[0] = true;
      return out;
    }

    // Nested maps
    if (value instanceof Map<?, ?> m) {
      Map<String, Object> out = new HashMap<>();
      for (Map.Entry<?, ?> entry : m.entrySet()) {
        Object key = entry.getKey();
        if (key != null) {
          out.put(String.valueOf(key), sanitizeValue(entry.getValue(), truncatedFlag));
        }
      }
      return out;
    }

    // Other numerics are fine
    if (value instanceof BigDecimal
        || value instanceof BigInteger
        || value instanceof Integer
        || value instanceof Long
        || value instanceof Short) {
      return value;
    }

    // Fallback: toString, with length guard
    String s = String.valueOf(value);
    if (s.length() > MAX_FIELD_STRING_LENGTH) {
      truncatedFlag[0] = true;
      return s.substring(0, MAX_FIELD_STRING_LENGTH) + "…";
    }
    return s;
  }

  private record SanitizeResult(List<Map<String, Object>> rows, boolean anyTruncated) {}
}
