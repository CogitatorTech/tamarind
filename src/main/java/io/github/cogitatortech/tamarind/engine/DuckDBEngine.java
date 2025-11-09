package io.github.cogitatortech.tamarind.engine;

import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.github.cogitatortech.tamarind.config.QueryConfig;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DuckDBEngine implements AnalyticalQueryEngine, JdbcQueryEngine {
  private static final Logger LOGGER = LogManager.getLogger(DuckDBEngine.class);
  // Fallback ThreadLocal for when TransactionManager is not available (for tests and CLI)
  // Must be cleaned up properly after each transaction to avoid leaking connections
  private final ThreadLocal<TransactionContext> fallbackContext =
      ThreadLocal.withInitial(() -> null);
  @Inject public QueryConfig queryConfig;
  @Inject public EngineConfig engineConfig;
  @Inject public DuckDBConnectionPool connectionPool;
  // Use Instance<T> for optional injection
  @Inject Instance<TransactionManager> transactionManagerInstance;

  public DuckDBEngine() {}

  private static String quoteIdentifier(String id) {
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException("Identifier cannot be null or empty");
    }
    String escaped = id.replace("\"", "\"\"");
    return '"' + escaped + '"';
  }

  private static String quoteStringLiteral(String value) {
    if (value == null) {
      return "NULL";
    }
    String escaped = value.replace("'", "''");
    return "'" + escaped + "'";
  }

  @Inject
  public void initialize() {
    if (transactionManagerInstance != null && !transactionManagerInstance.isUnsatisfied()) {
      LOGGER.info("DuckDBEngine initialized with TransactionManager - using connection pool");
    } else {
      LOGGER.info(
          "DuckDBEngine initialized without TransactionManager (fallback mode) - using connection pool");
    }
  }

  @Override
  public void close() throws SQLException {
    // Clean up fallback context if used
    TransactionContext ctx = fallbackContext.get();
    if (ctx != null) {
      try {
        ctx.close();
      } catch (SQLException e) {
        LOGGER.warn("Error closing fallback transaction context", e);
      }
      fallbackContext.remove();
    }
    LOGGER.info("DuckDBEngine closed");
  }

  @Override
  public QueryResult executeQuery(String sql) throws SQLException {
    LOGGER.debug("Executing query: {}", sql);
    List<Map<String, Object>> rows = new ArrayList<>();
    List<String> columnNames = new ArrayList<>();

    // Use transaction connection if active (prefer TransactionManager, fallback to ThreadLocal)
    TransactionContext ctx = getActiveTransactionContext();
    Connection conn;
    boolean shouldCloseConnection;

    if (ctx != null && ctx.isActive()) {
      conn = ctx.getConnection();
      shouldCloseConnection = false;
    } else {
      conn = connectionPool.getConnection();
      shouldCloseConnection = true;
    }

    try (Statement stmt = conn.createStatement()) {

      if (queryConfig != null && queryConfig.timeoutSeconds().isPresent()) {
        int timeout = queryConfig.timeoutSeconds().get();
        stmt.setQueryTimeout(timeout);
        LOGGER.debug("Query timeout set to {} seconds", timeout);
      }

      try (ResultSet rs = stmt.executeQuery(sql)) {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
          columnNames.add(metaData.getColumnLabel(i));
        }

        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= columnCount; i++) {
            row.put(metaData.getColumnLabel(i), rs.getObject(i));
          }
          rows.add(row);
        }
      }
    } finally {
      if (shouldCloseConnection && conn != null) {
        conn.close();
      }
    }

    LOGGER.debug("Query returned {} rows", rows.size());
    return new QueryResult(columnNames, rows);
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    LOGGER.debug("Executing update: {}", sql);

    // Use transaction connection if active (prefer TransactionManager, fallback to ThreadLocal)
    TransactionContext ctx = getActiveTransactionContext();
    Connection conn;
    boolean shouldCloseConnection;

    if (ctx != null && ctx.isActive()) {
      conn = ctx.getConnection();
      shouldCloseConnection = false;
    } else {
      conn = connectionPool.getConnection();
      shouldCloseConnection = true;
    }

    try (Statement stmt = conn.createStatement()) {
      if (queryConfig != null && queryConfig.timeoutSeconds().isPresent()) {
        int timeout = queryConfig.timeoutSeconds().get();
        stmt.setQueryTimeout(timeout);
        LOGGER.debug("Query timeout set to {} seconds for update", timeout);
      }

      return stmt.executeUpdate(sql);
    } finally {
      if (shouldCloseConnection && conn != null) {
        conn.close();
      }
    }
  }

  @Override
  public void createViewFromFile(String viewName, String filePath) throws SQLException {
    // Quote the identifier to avoid injection and allow unusual names
    String quotedView = quoteIdentifier(viewName);

    // Escape the file path as a SQL string literal
    String fileLiteral = quoteStringLiteral(filePath);

    // Determine the appropriate DuckDB table function based on file extension
    String lower = filePath == null ? "" : filePath.toLowerCase();
    String tableFunction;
    if (lower.endsWith(".parquet")) {
      tableFunction = "read_parquet(" + fileLiteral + ")";
    } else if (lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt")) {
      tableFunction = "read_csv_auto(" + fileLiteral + ")";
    } else if (lower.endsWith(".json") || lower.endsWith(".ndjson")) {
      tableFunction = "read_json_auto(" + fileLiteral + ")";
    } else if (lower.endsWith(".arrow") || lower.endsWith(".ipc") || lower.endsWith(".feather")) {
      tableFunction = "read_arrow(" + fileLiteral + ")";
    } else if (lower.endsWith(".orc")) {
      tableFunction = "read_orc(" + fileLiteral + ")";
    } else {
      // Fallback to DuckDB's auto scanner when extension is unknown
      tableFunction = "read_autodetect(" + fileLiteral + ")";
    }

    String sql =
        String.format("CREATE OR REPLACE VIEW %s AS SELECT * FROM %s", quotedView, tableFunction);

    try (Connection conn = connectionPool.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(sql);
    }

    LOGGER.info("Created view '{}' from file: {}", viewName, filePath);
  }

  @Override
  public boolean isHealthy() {
    return connectionPool.isHealthy();
  }

  @Override
  public String getEngineName() {
    return "DuckDB";
  }

  @Override
  public String getEngineVersion() {
    try (Connection conn = connectionPool.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT library_version FROM pragma_version()")) {
      if (rs.next()) {
        return rs.getString(1);
      }
    } catch (SQLException e) {
      LOGGER.error("Failed to get DuckDB version", e);
    }
    return "unknown";
  }

  @Override
  public void beginTransaction() throws SQLException {
    // Prefer TransactionManager if available
    TransactionManager txManager = getTransactionManager();
    if (txManager != null) {
      if (txManager.isTransactionActive()) {
        throw new SQLException("Transaction already active");
      }
      Connection conn = connectionPool.getConnection();
      TransactionContext ctx = new TransactionContext(conn);
      txManager.beginTransaction(ctx);
      LOGGER.debug("Transaction started with TransactionManager");
    } else {
      // Fallback to ThreadLocal for tests/CLI
      if (fallbackContext.get() != null) {
        throw new SQLException("Transaction already active");
      }
      Connection conn = connectionPool.getConnection();
      TransactionContext ctx = new TransactionContext(conn);
      fallbackContext.set(ctx);
      LOGGER.debug("Transaction started with fallback mode");
    }
  }

  @Override
  public void commit() throws SQLException {
    TransactionManager txManager = getTransactionManager();
    if (txManager != null) {
      txManager.commit();
    } else {
      // Fallback mode
      TransactionContext ctx = fallbackContext.get();
      if (ctx == null) {
        throw new SQLException("No active transaction");
      }
      try {
        ctx.commit();
        LOGGER.debug("Transaction committed (fallback mode)");
      } finally {
        ctx.close();
        fallbackContext.remove();
      }
    }
  }

  @Override
  public void rollback() throws SQLException {
    TransactionManager txManager = getTransactionManager();
    if (txManager != null) {
      txManager.rollback();
    } else {
      // Fallback mode
      TransactionContext ctx = fallbackContext.get();
      if (ctx == null) {
        throw new SQLException("No active transaction");
      }
      try {
        ctx.rollback();
        LOGGER.debug("Transaction rolled back (fallback mode)");
      } finally {
        ctx.close();
        fallbackContext.remove();
      }
    }
  }

  @Override
  public boolean isTransactionActive() {
    TransactionManager txManager = getTransactionManager();
    if (txManager != null) {
      return txManager.isTransactionActive();
    } else {
      TransactionContext ctx = fallbackContext.get();
      return ctx != null && ctx.isActive();
    }
  }

  /** Get the active transaction context from TransactionManager or fallback ThreadLocal. */
  private TransactionContext getActiveTransactionContext() {
    TransactionManager txManager = getTransactionManager();
    if (txManager != null) {
      return txManager.getActiveContext();
    } else {
      return fallbackContext.get();
    }
  }

  /** Get TransactionManager if available. */
  private TransactionManager getTransactionManager() {
    if (transactionManagerInstance != null && !transactionManagerInstance.isUnsatisfied()) {
      return transactionManagerInstance.get();
    }
    return null;
  }

  @Override
  public Connection getJdbcConnection() throws SQLException {
    return connectionPool.getConnection();
  }

  @Override
  public DatabaseMetadata getMetadata() {
    return new JdbcDatabaseMetadata(this);
  }

  // AnalyticalQueryEngine implementation
  @Override
  public String[] getSupportedFileFormats() {
    return new String[] {"parquet", "csv", "json", "arrow", "orc"};
  }

  @Override
  public boolean supportsDirectFileQuery() {
    return true;
  }

  @Override
  public boolean isColumnar() {
    return true;
  }

  /** JDBC-based metadata implementation. */
  private static class JdbcDatabaseMetadata implements DatabaseMetadata {
    private final JdbcQueryEngine engine;

    JdbcDatabaseMetadata(JdbcQueryEngine engine) {
      this.engine = engine;
    }

    @Override
    public List<String> getTables() throws SQLException {
      List<String> tables = new ArrayList<>();
      try (Connection conn = engine.getJdbcConnection();
          ResultSet rs =
              conn.getMetaData().getTables(null, null, "%", new String[] {"TABLE", "VIEW"})) {
        while (rs.next()) {
          tables.add(rs.getString("TABLE_NAME"));
        }
      }
      return tables;
    }

    @Override
    public List<String> getColumns(String tableName) throws SQLException {
      List<String> columns = new ArrayList<>();
      try (Connection conn = engine.getJdbcConnection();
          ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
        while (rs.next()) {
          columns.add(rs.getString("COLUMN_NAME"));
        }
      }
      return columns;
    }

    @Override
    public List<ColumnMeta> getColumnsWithTypes(String tableName) throws SQLException {
      List<ColumnMeta> cols = new ArrayList<>();
      try (Connection conn = engine.getJdbcConnection();
          ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
        while (rs.next()) {
          String name = rs.getString("COLUMN_NAME");
          String typeName = rs.getString("TYPE_NAME");
          cols.add(new ColumnMeta(name, typeName));
        }
      }
      return cols;
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
      try (Connection conn = engine.getJdbcConnection()) {
        return conn.getMetaData().getDatabaseProductName();
      }
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
      try (Connection conn = engine.getJdbcConnection()) {
        return conn.getMetaData().getDatabaseProductVersion();
      }
    }
  }
}
