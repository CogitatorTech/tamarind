package io.github.cogitatortech.tamarind.engine;

import jakarta.enterprise.context.RequestScoped;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Request-scoped transaction manager that safely manages transactions without ThreadLocal.
 *
 * <p>This bean is created per request and automatically cleaned up when the request ends,
 * preventing memory leaks in pooled thread environments.
 *
 * <p>Usage in REST endpoints:
 *
 * <pre>
 * {@literal @}Inject TransactionManager txManager;
 *
 * txManager.executeInTransaction(engine -> {
 *     engine.executeUpdate("INSERT ...");
 *     engine.executeUpdate("UPDATE ...");
 * });
 * </pre>
 */
@RequestScoped
public class TransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(TransactionManager.class);

  private TransactionContext activeContext;

  /**
   * Get the active transaction context, or null if no transaction is active.
   *
   * @return the active transaction context, or null
   */
  public TransactionContext getActiveContext() {
    return activeContext;
  }

  /**
   * Check if a transaction is currently active in this request scope.
   *
   * @return true if transaction is active
   */
  public boolean isTransactionActive() {
    return activeContext != null && activeContext.isActive();
  }

  /**
   * Begin a new transaction. Must be paired with commit() or rollback().
   *
   * @param context the transaction context
   * @throws SQLException if transaction already active
   */
  public void beginTransaction(TransactionContext context) throws SQLException {
    if (activeContext != null && activeContext.isActive()) {
      throw new SQLException("Transaction already active in this request");
    }
    this.activeContext = context;
    LOGGER.debug("Transaction started in request scope");
  }

  /**
   * Commit the active transaction.
   *
   * @throws SQLException if no transaction is active
   */
  public void commit() throws SQLException {
    if (activeContext == null) {
      throw new SQLException("No active transaction");
    }
    try {
      activeContext.commit();
      LOGGER.debug("Transaction committed");
    } finally {
      cleanup();
    }
  }

  /**
   * Rollback the active transaction.
   *
   * @throws SQLException if no transaction is active
   */
  public void rollback() throws SQLException {
    if (activeContext == null) {
      throw new SQLException("No active transaction");
    }
    try {
      activeContext.rollback();
      LOGGER.debug("Transaction rolled back");
    } finally {
      cleanup();
    }
  }

  /**
   * Execute a block of code within a transaction. Automatically commits on success or rolls back on
   * exception.
   *
   * @param logic the transaction logic to execute
   * @param engine the query engine
   * @throws SQLException if transaction fails
   */
  public void executeInTransaction(TransactionLogic logic, QueryEngine engine) throws SQLException {
    if (!(engine instanceof DuckDBEngine)) {
      throw new SQLException("Transaction support requires DuckDBEngine");
    }

    DuckDBEngine duckEngine = (DuckDBEngine) engine;
    TransactionContext ctx = null;

    try {
      ctx = new TransactionContext(duckEngine.connectionPool.getConnection());
      beginTransaction(ctx);
      logic.execute(engine);
      commit();
    } catch (Exception e) {
      if (activeContext != null) {
        try {
          rollback();
        } catch (SQLException rollbackEx) {
          LOGGER.error("Failed to rollback transaction", rollbackEx);
          e.addSuppressed(rollbackEx);
        }
      }
      throw new SQLException("Transaction failed", e);
    } finally {
      cleanup();
    }
  }

  /** Clean up transaction resources. */
  private void cleanup() {
    if (activeContext != null) {
      try {
        activeContext.close();
      } catch (SQLException e) {
        LOGGER.warn("Error closing transaction context", e);
      }
      activeContext = null;
    }
  }

  /** Functional interface for transaction logic. */
  @FunctionalInterface
  public interface TransactionLogic {
    void execute(QueryEngine engine) throws Exception;
  }
}
