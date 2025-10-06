package io.github.cogitatortech.tamarind.engine;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe transaction context that manages database transactions.
 *
 * <p>This class replaces ThreadLocal usage to prevent memory leaks in pooled thread environments
 * (e.g., Vert.x event loops).
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Multiple threads can safely call
 * commit()/rollback() though only one will succeed.
 *
 * <p>Usage:
 *
 * <pre>
 * try (TransactionContext tx = new TransactionContext(connection)) {
 *   // Execute queries
 *   tx.commit();
 * } // Auto-rollback on exception
 * </pre>
 */
public class TransactionContext implements AutoCloseable {
  private static final Logger LOGGER = LogManager.getLogger(TransactionContext.class);

  private final Connection connection;
  private volatile boolean committed = false;
  private volatile boolean rolledBack = false;

  public TransactionContext(Connection connection) throws SQLException {
    this.connection = connection;
    this.connection.setAutoCommit(false);
    LOGGER.debug("Transaction started");
  }

  public Connection getConnection() {
    return connection;
  }

  public synchronized void commit() throws SQLException {
    if (committed) {
      throw new IllegalStateException("Transaction already committed");
    }
    if (rolledBack) {
      throw new IllegalStateException("Transaction already rolled back");
    }

    connection.commit();
    committed = true;
    LOGGER.debug("Transaction committed");
  }

  public synchronized void rollback() throws SQLException {
    if (committed) {
      throw new IllegalStateException("Cannot rollback committed transaction");
    }
    if (rolledBack) {
      return; // Already rolled back
    }

    connection.rollback();
    rolledBack = true;
    LOGGER.debug("Transaction rolled back");
  }

  public boolean isActive() {
    return !committed && !rolledBack;
  }

  @Override
  public synchronized void close() throws SQLException {
    try {
      // Auto-rollback if transaction not explicitly committed
      if (!committed && !rolledBack) {
        LOGGER.warn("Transaction not committed, performing auto-rollback");
        connection.rollback();
      }
    } finally {
      connection.setAutoCommit(true);
      connection.close();
    }
  }
}
