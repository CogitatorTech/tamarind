package io.github.cogitatortech.tamarind.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks running query threads for naive cancellation. This is a best-effort approach and relies on
 * the engine respecting interrupts/timeouts.
 *
 * <p><b>IMPORTANT LIMITATIONS:</b>
 *
 * <ul>
 *   <li>Thread interruption does NOT stop JDBC queries in most databases including DuckDB
 *   <li>This only interrupts the thread; the database query continues executing
 *   <li>Use query timeouts (via QueryConfig) for reliable query cancellation
 *   <li>Consider using Statement.cancel() for true database-level cancellation
 * </ul>
 *
 * <p>This mechanism is primarily useful for interrupting application-level processing after a query
 * completes, not for stopping the query itself.
 */
@ApplicationScoped
public class QueryExecutionManager {

  private final Map<String, Thread> running = new ConcurrentHashMap<>();

  public void track(String id, Thread thread) {
    running.put(id, thread);
  }

  public void untrack(String id) {
    running.remove(id);
  }

  public boolean cancelQuery(String id) {
    Thread t = running.get(id);
    if (t != null) {
      t.interrupt();
      return true;
    }
    return false;
  }
}
