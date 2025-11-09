package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransactionManagerIntegrationTest {

  private DuckDBConnectionPool pool;
  private DuckDBEngine engine;

  @BeforeEach
  public void setUp() throws Exception {
    pool = new DuckDBConnectionPool();
    // create minimal poolConfig and engineConfig via setters
    io.github.cogitatortech.tamarind.config.ConnectionPoolConfig pconf =
        new io.github.cogitatortech.tamarind.config.ConnectionPoolConfig() {
          public Integer maximumPoolSize() {
            return 5;
          }

          public Integer minimumIdle() {
            return 1;
          }

          public Long connectionTimeout() {
            return 30000L;
          }

          public Long idleTimeout() {
            return 600000L;
          }

          public Long maxLifetime() {
            return 1800000L;
          }

          public String poolName() {
            return "TestPool";
          }
        };
    io.github.cogitatortech.tamarind.config.EngineConfig econf =
        new io.github.cogitatortech.tamarind.config.EngineConfig() {
          public String type() {
            return "duckdb";
          }

          public String databasePath() {
            return ":memory:";
          }

          public java.util.Optional<
                  io.github.cogitatortech.tamarind.config.EngineConfig.DuckDBConfig>
              duckdb() {
            return java.util.Optional.empty();
          }
        };
    pool.setPoolConfig(pconf);
    pool.setEngineConfig(econf);
    pool.initialize();

    engine = new DuckDBEngine();
    // set injected fields manually
    engine.connectionPool = pool;
    // transactionManagerInstance empty - use null to force fallback mode
    engine.transactionManagerInstance = null;
    engine.initialize();
  }

  @AfterEach
  public void tearDown() throws Exception {
    pool.cleanup();
  }

  @Test
  public void transaction_commit_and_rollback_work_in_fallbackMode() throws Exception {
    // Create a table and insert within a transaction using fallback ThreadLocal
    engine.beginTransaction();
    try {
      engine.executeUpdate("CREATE TABLE IF NOT EXISTS t(id INTEGER);");
      engine.executeUpdate("INSERT INTO t VALUES (1);");
      engine.commit();
    } catch (Exception e) {
      engine.rollback();
      throw e;
    }

    // Verify row exists
    try (Connection conn = pool.getConnection();
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM t")) {
      assertTrue(rs.next());
      assertEquals(1, rs.getInt(1));
    }

    // Now test rollback
    engine.beginTransaction();
    try {
      engine.executeUpdate("INSERT INTO t VALUES (2);");
      engine.rollback();
    } catch (Exception e) {
      engine.rollback();
      throw e;
    }

    try (Connection conn = pool.getConnection();
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM t")) {
      assertTrue(rs.next());
      assertEquals(1, rs.getInt(1));
    }
  }
}
