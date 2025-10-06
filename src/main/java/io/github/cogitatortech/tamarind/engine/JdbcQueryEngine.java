package io.github.cogitatortech.tamarind.engine;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Internal interface for JDBC-based query engines. This should NOT be part of the public
 * QueryEngine interface to allow for non-JDBC implementations.
 *
 * <p>Only internal components (like Arrow Flight) that need direct JDBC access should use this.
 */
public interface JdbcQueryEngine extends QueryEngine {

  /**
   * Get a JDBC connection for advanced operations. This method is for internal use only and should
   * not be exposed to external clients.
   *
   * <p>IMPORTANT: Callers must close the connection when done.
   *
   * @return JDBC connection
   * @throws SQLException if connection cannot be obtained
   */
  Connection getJdbcConnection() throws SQLException;
}
