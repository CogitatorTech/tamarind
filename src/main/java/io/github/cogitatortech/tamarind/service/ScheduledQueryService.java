package io.github.cogitatortech.tamarind.service;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Service for scheduled query execution. Allows queries to be scheduled at regular intervals. */
@ApplicationScoped
public class ScheduledQueryService {

  private static final Logger LOGGER = LogManager.getLogger(ScheduledQueryService.class);
  private final Map<String, ScheduledQuery> scheduledQueries = new ConcurrentHashMap<>();
  @Inject QueryEngine engine;

  /**
   * Register a query to be executed on a schedule.
   *
   * @param queryId Unique identifier for the scheduled query
   * @param sql The SQL query to execute
   * @param cronExpression Cron expression for scheduling
   */
  public void registerScheduledQuery(String queryId, String sql, String cronExpression) {
    ScheduledQuery query = new ScheduledQuery(queryId, sql, cronExpression);
    scheduledQueries.put(queryId, query);
    LOGGER.info("Registered scheduled query: {} with cron: {}", queryId, cronExpression);
  }

  /** Unregister a scheduled query. */
  public void unregisterScheduledQuery(String queryId) {
    scheduledQueries.remove(queryId);
    LOGGER.info("Unregistered scheduled query: {}", queryId);
  }

  /** Execute all scheduled queries (called by scheduler). */
  @Scheduled(every = "1m")
  void executeScheduledQueries() {
    if (scheduledQueries.isEmpty()) {
      return;
    }

    LOGGER.debug("Checking scheduled queries...");
    for (ScheduledQuery query : scheduledQueries.values()) {
      try {
        LOGGER.info("Executing scheduled query: {}", query.queryId);
        QueryResult result = engine.executeQuery(query.sql);
        query.lastResult = result;
        query.lastExecutionTime = System.currentTimeMillis();
        query.executionCount++;
        LOGGER.info(
            "Scheduled query {} completed: {} rows", query.queryId, result.getRows().size());
      } catch (SQLException e) {
        LOGGER.error("Error executing scheduled query {}: {}", query.queryId, e.getMessage(), e);
        query.lastError = e.getMessage();
      }
    }
  }

  /** Get the result of the last execution for a scheduled query. */
  public QueryResult getLastResult(String queryId) {
    ScheduledQuery query = scheduledQueries.get(queryId);
    return query != null ? query.lastResult : null;
  }

  /** Get all scheduled queries. */
  public List<ScheduledQueryInfo> getAllScheduledQueries() {
    List<ScheduledQueryInfo> infos = new ArrayList<>();
    for (ScheduledQuery query : scheduledQueries.values()) {
      infos.add(
          new ScheduledQueryInfo(
              query.queryId,
              query.sql,
              query.cronExpression,
              query.executionCount,
              query.lastExecutionTime,
              query.lastError));
    }
    return infos;
  }

  /** Internal class to hold scheduled query information. */
  private static class ScheduledQuery {
    final String queryId;
    final String sql;
    final String cronExpression;
    QueryResult lastResult;
    long lastExecutionTime;
    int executionCount;
    String lastError;

    ScheduledQuery(String queryId, String sql, String cronExpression) {
      this.queryId = queryId;
      this.sql = sql;
      this.cronExpression = cronExpression;
    }
  }

  /** Public information about a scheduled query. */
  public record ScheduledQueryInfo(
      String queryId,
      String sql,
      String cronExpression,
      int executionCount,
      long lastExecutionTime,
      String lastError) {}
}
