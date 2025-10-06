package io.github.cogitatortech.tamarind.engine;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Async/reactive wrapper for QueryEngine to enable non-blocking query execution. This class enables
 * handling >1000 concurrent users by executing queries on a worker thread pool without blocking the
 * event loop.
 */
@ApplicationScoped
public class AsyncQueryEngine {

  private static final Logger LOGGER = LogManager.getLogger(AsyncQueryEngine.class);

  @Inject QueryEngine engine;

  /**
   * Execute a query asynchronously without blocking the event loop.
   *
   * @param sql SQL query to execute
   * @return Uni wrapping the QueryResult
   */
  public Uni<QueryResult> executeQueryAsync(String sql) {
    return Uni.createFrom()
        .item(
            () -> {
              try {
                LOGGER.debug("Executing async query: {}", sql);
                return engine.executeQuery(sql);
              } catch (SQLException e) {
                throw new RuntimeException("Query execution failed", e);
              }
            })
        .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
  }

  /**
   * Execute an update asynchronously without blocking the event loop.
   *
   * @param sql SQL update/insert/delete statement to execute
   * @return Uni wrapping the number of affected rows
   */
  public Uni<Integer> executeUpdateAsync(String sql) {
    return Uni.createFrom()
        .item(
            () -> {
              try {
                LOGGER.debug("Executing async update: {}", sql);
                return engine.executeUpdate(sql);
              } catch (SQLException e) {
                throw new RuntimeException("Update execution failed", e);
              }
            })
        .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
  }

  /**
   * Execute a transaction asynchronously.
   *
   * @param transactionLogic Function that executes transaction logic
   * @return Uni indicating completion
   */
  public Uni<Void> executeTransactionAsync(TransactionLogic transactionLogic) {
    return Uni.createFrom()
        .voidItem()
        .invoke(
            () -> {
              try {
                engine.beginTransaction();
                try {
                  transactionLogic.execute(engine);
                  engine.commit();
                  LOGGER.debug("Async transaction committed successfully");
                } catch (Exception e) {
                  engine.rollback();
                  LOGGER.warn("Async transaction rolled back due to error", e);
                  throw e;
                }
              } catch (SQLException e) {
                throw new RuntimeException("Transaction execution failed", e);
              }
            })
        .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
  }

  /**
   * Check if the engine is healthy asynchronously.
   *
   * @return Uni wrapping health status
   */
  public Uni<Boolean> isHealthyAsync() {
    return Uni.createFrom()
        .item(() -> engine.isHealthy())
        .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
  }

  /** Functional interface for transaction logic. */
  @FunctionalInterface
  public interface TransactionLogic {
    void execute(QueryEngine engine) throws SQLException;
  }
}
