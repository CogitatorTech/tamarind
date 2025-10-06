package io.github.cogitatortech.tamarind.rest;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import io.github.cogitatortech.tamarind.security.AuditLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/api/v1/query/batch")
public class BatchQueryResource {

  private static final Logger LOGGER = LogManager.getLogger(BatchQueryResource.class);

  @Inject QueryEngine engine;

  @Inject AuditLogger auditLogger;

  @POST
  @Consumes("application/json")
  @Produces("application/json")
  public BatchQueryResponse executeBatch(
      BatchQueryRequest request, @Context SecurityContext securityContext) {
    String userId = getUserId(securityContext);
    LOGGER.info(
        "Executing batch query request with {} queries for user {}",
        request.queries.size(),
        userId);

    List<QueryResultResponse> results = new ArrayList<>();
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < request.queries.size(); i++) {
      String sql = request.queries.get(i);
      QueryResultResponse queryResponse = new QueryResultResponse();
      queryResponse.queryIndex = i;
      queryResponse.sql = sql;

      try {
        long queryStart = System.currentTimeMillis();
        QueryResult result = engine.executeQuery(sql);
        long executionTime = System.currentTimeMillis() - queryStart;

        queryResponse.success = true;
        queryResponse.rows = result.getRows();
        queryResponse.columnNames = result.getColumnNames();
        queryResponse.executionTimeMs = executionTime;

        auditLogger.logQuery(userId, sql, executionTime, result.getRows().size());
      } catch (SQLException e) {
        LOGGER.error("Error executing query {}: {}", i, e.getMessage(), e);
        queryResponse.success = false;
        queryResponse.error = e.getMessage();
        auditLogger.logError(userId, sql, e.getMessage());
      }

      results.add(queryResponse);
    }

    long totalTime = System.currentTimeMillis() - startTime;
    LOGGER.info("Batch query completed in {}ms", totalTime);

    BatchQueryResponse response = new BatchQueryResponse();
    response.results = results;
    response.totalExecutionTimeMs = totalTime;
    return response;
  }

  private String getUserId(SecurityContext securityContext) {
    if (securityContext != null && securityContext.getUserPrincipal() != null) {
      return securityContext.getUserPrincipal().getName();
    }
    return "anonymous";
  }

  public static class BatchQueryRequest {
    public List<String> queries;
  }

  public static class BatchQueryResponse {
    public List<QueryResultResponse> results;
    public long totalExecutionTimeMs;
  }

  public static class QueryResultResponse {
    public int queryIndex;
    public String sql;
    public boolean success;
    public List<String> columnNames;
    public List<Map<String, Object>> rows;
    public String error;
    public long executionTimeMs;
  }
}
