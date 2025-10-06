package io.github.cogitatortech.tamarind.engine;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cogitatortech.tamarind.config.ConnectionPoolConfig;
import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class DuckDBConnectionPool {

  private static final Logger LOGGER = LogManager.getLogger(DuckDBConnectionPool.class);
  @Inject private ConnectionPoolConfig poolConfig;
  @Inject private EngineConfig engineConfig;
  private HikariDataSource dataSource;

  public EngineConfig getEngineConfig() {
    return this.engineConfig;
  }

  // Accessors for CLI/tests
  public void setEngineConfig(EngineConfig engineConfig) {
    this.engineConfig = engineConfig;
  }

  public ConnectionPoolConfig getPoolConfig() {
    return this.poolConfig;
  }

  public void setPoolConfig(ConnectionPoolConfig poolConfig) {
    this.poolConfig = poolConfig;
  }

  @PostConstruct
  public void initialize() {
    LOGGER.info("Initializing DuckDB connection pool...");

    HikariConfig config = new HikariConfig();

    // Normalize in-memory configuration: DuckDB uses "jdbc:duckdb:" for in-memory
    String dbPath = engineConfig.databasePath();
    String jdbcUrl;
    if (dbPath == null
        || dbPath.isBlank()
        || ":memory:".equalsIgnoreCase(dbPath)
        || "memory".equalsIgnoreCase(dbPath)) {
      jdbcUrl = "jdbc:duckdb:"; // in-memory
    } else {
      jdbcUrl = "jdbc:duckdb:" + dbPath; // file-backed
    }

    // DuckDB configuration via PRAGMA statements (executed after connection)
    // URL parameters are NOT supported by DuckDB JDBC driver and will be treated as part of
    // filename
    StringBuilder connectionInitSql = new StringBuilder();
    if (engineConfig.duckdb().isPresent()) {
      EngineConfig.DuckDBConfig duckdbConfig = engineConfig.duckdb().get();

      // Note: access_mode must be set before any other operations
      // It cannot be changed after database is opened
      if (duckdbConfig.accessMode() != null && !duckdbConfig.accessMode().isBlank()) {
        // For access_mode, we need to append it as a config parameter (DuckDB specific syntax)
        // Format: jdbc:duckdb:path?access_mode=read_write is NOT supported
        // Instead, use connection properties or accept default
        LOGGER.debug(
            "Access mode '{}' will use default DuckDB behavior", duckdbConfig.accessMode());
      }

      // Threads can be set via PRAGMA after connection
      if (duckdbConfig.threads() != null && duckdbConfig.threads() > 0) {
        if (connectionInitSql.length() > 0) {
          connectionInitSql.append("; ");
        }
        connectionInitSql.append("PRAGMA threads=").append(duckdbConfig.threads());
      }
    }

    // Explicitly set the DuckDB driver to avoid DriverManager lookup timing issues
    config.setDriverClassName("org.duckdb.DuckDBDriver");
    config.setJdbcUrl(jdbcUrl);
    config.setMaximumPoolSize(poolConfig.maximumPoolSize());
    config.setMinimumIdle(poolConfig.minimumIdle());
    config.setConnectionTimeout(poolConfig.connectionTimeout());
    config.setIdleTimeout(poolConfig.idleTimeout());
    config.setMaxLifetime(poolConfig.maxLifetime());
    config.setPoolName(poolConfig.poolName());

    // Set connection initialization SQL if we have any PRAGMA statements
    if (connectionInitSql.length() > 0) {
      config.setConnectionInitSql(connectionInitSql.toString());
      LOGGER.info("DuckDB connection init SQL: {}", connectionInitSql);
    }

    dataSource = new HikariDataSource(config);
    LOGGER.info(
        "DuckDB connection pool initialized with {} max connections", config.getMaximumPoolSize());
  }

  @PreDestroy
  public void cleanup() {
    if (dataSource != null && !dataSource.isClosed()) {
      LOGGER.info("Closing DuckDB connection pool...");
      dataSource.close();
      LOGGER.info("DuckDB connection pool closed");
    }
  }

  public void onStop(@Observes ShutdownEvent event) {
    cleanup();
  }

  public Connection getConnection() throws SQLException {
    if (dataSource == null || dataSource.isClosed()) {
      throw new SQLException("Connection pool is not initialized or has been closed");
    }
    return dataSource.getConnection();
  }

  public String getStats() {
    if (dataSource == null) {
      return "Pool not initialized";
    }
    return String.format(
        "Active: %d, Idle: %d, Total: %d, Waiting: %d",
        dataSource.getHikariPoolMXBean().getActiveConnections(),
        dataSource.getHikariPoolMXBean().getIdleConnections(),
        dataSource.getHikariPoolMXBean().getTotalConnections(),
        dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
  }

  public boolean isHealthy() {
    if (dataSource == null || dataSource.isClosed()) {
      return false;
    }
    try (Connection conn = dataSource.getConnection();
        java.sql.Statement stmt = conn.createStatement();
        java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
      // Actually execute a query to validate database is working
      return rs.next() && rs.getInt(1) == 1;
    } catch (SQLException e) {
      LOGGER.error("Connection pool health check failed", e);
      return false;
    }
  }
}
