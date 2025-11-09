package io.github.cogitatortech.tamarind.engine;

import java.sql.SQLException;
import java.util.List;

/**
 * QueryEngine interface - database-agnostic query execution.
 *
 * <p>This interface does NOT expose JDBC-specific details, allowing for non-JDBC implementations
 * (HTTP APIs, gRPC backends, etc.)
 */
public interface QueryEngine extends AutoCloseable {

  QueryResult executeQuery(String sql) throws SQLException;

  int executeUpdate(String sql) throws SQLException;

  void createViewFromFile(String viewName, String filePath) throws SQLException;

  boolean isHealthy();

  String getEngineName();

  String getEngineVersion();

  /**
   * Get database metadata (tables, columns, etc.) without exposing JDBC Connection.
   *
   * @return metadata interface for schema inspection
   */
  DatabaseMetadata getMetadata();

  /**
   * Begin a new transaction. After calling this method, all subsequent executeQuery and
   * executeUpdate calls will be part of the transaction until commit() or rollback() is called.
   *
   * @throws SQLException if a database access error occurs
   */
  void beginTransaction() throws SQLException;

  /**
   * Commit the current transaction, making all changes permanent.
   *
   * @throws SQLException if a database access error occurs or no transaction is active
   */
  void commit() throws SQLException;

  /**
   * Rollback the current transaction, undoing all changes.
   *
   * @throws SQLException if a database access error occurs or no transaction is active
   */
  void rollback() throws SQLException;

  /**
   * Check if a transaction is currently active.
   *
   * @return true if a transaction is active, false otherwise
   */
  boolean isTransactionActive();

  /** Database metadata interface - provides schema information without exposing JDBC. */
  interface DatabaseMetadata {
    List<String> getTables() throws SQLException;

    List<String> getColumns(String tableName) throws SQLException;

    /**
     * Return detailed column metadata (name + type). Default implementation falls back to simple
     * names only. Implementations override for richer metadata without requiring DESCRIBE queries.
     *
     * @param tableName table identifier
     * @return list of ColumnMeta objects (name, type); never null
     * @throws SQLException on metadata access error
     */
    default List<ColumnMeta> getColumnsWithTypes(String tableName) throws SQLException {
      var names = getColumns(tableName);
      var list = new java.util.ArrayList<ColumnMeta>(names.size());
      for (String n : names) list.add(new ColumnMeta(n, null));
      return list;
    }

    String getDatabaseProductName() throws SQLException;

    String getDatabaseProductVersion() throws SQLException;
  }

  /** Simple column metadata record. */
  class ColumnMeta {
    private final String name;
    private final String type; // nullable type string

    public ColumnMeta(String name, String type) {
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }
  }
}
