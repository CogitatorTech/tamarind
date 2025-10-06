package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.config.ConnectionPoolConfig;
import io.github.cogitatortech.tamarind.config.EngineConfig;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for DuckDBEngine. */
class DuckDBEngineTest {

  private DuckDBEngine engine;
  private DuckDBConnectionPool connectionPool;

  @BeforeEach
  void setUp() {
    // Create real connection pool for testing with in-memory database
    EngineConfig engineConfig = mock(EngineConfig.class);
    when(engineConfig.databasePath()).thenReturn(":memory:");
    when(engineConfig.duckdb()).thenReturn(java.util.Optional.empty());

    ConnectionPoolConfig poolConfig = mock(ConnectionPoolConfig.class);
    when(poolConfig.maximumPoolSize()).thenReturn(2);
    when(poolConfig.minimumIdle()).thenReturn(0);
    when(poolConfig.connectionTimeout()).thenReturn(30000L);
    when(poolConfig.idleTimeout()).thenReturn(600000L);
    when(poolConfig.maxLifetime()).thenReturn(1800000L);
    when(poolConfig.poolName()).thenReturn("TestPool");

    connectionPool = new DuckDBConnectionPool();
    connectionPool.setEngineConfig(engineConfig);
    connectionPool.setPoolConfig(poolConfig);
    connectionPool.initialize();

    engine = new DuckDBEngine();
    engine.connectionPool = connectionPool;
    engine.initialize();
  }

  @AfterEach
  void tearDown() {
    if (connectionPool != null) {
      connectionPool.cleanup();
    }
  }

  @Test
  void testBasicQuery() throws SQLException {
    QueryResult result = engine.executeQuery("SELECT 42 as answer, 'Hello' as greeting");

    assertEquals(1, result.getRowCount());
    assertEquals(2, result.getColumnCount());
    assertEquals(42, result.getRows().get(0).get("answer"));
    assertEquals("Hello", result.getRows().get(0).get("greeting"));
  }

  @Test
  void testMultipleRows() throws SQLException {
    String sql =
        "SELECT * FROM (VALUES (1, 'Alice'), (2, 'Bob'), (3, 'Charlie')) AS t(id," + " name)";
    QueryResult result = engine.executeQuery(sql);

    assertEquals(3, result.getRowCount());
    assertEquals(2, result.getColumnCount());
  }

  @Test
  void testCreateTable() throws SQLException {
    engine.executeUpdate(
        "CREATE TABLE test_data AS SELECT * FROM (VALUES (1, 'A'), (2, 'B')) AS" + " t(id, value)");

    QueryResult result = engine.executeQuery("SELECT * FROM test_data");
    assertEquals(2, result.getRowCount());
  }
}
