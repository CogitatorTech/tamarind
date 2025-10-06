package io.github.cogitatortech.tamarind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.config.ConnectionPoolConfig;
import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.github.cogitatortech.tamarind.engine.DuckDBConnectionPool;
import io.github.cogitatortech.tamarind.engine.DuckDBEngine;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduledQueryServiceTest {

  private ScheduledQueryService service;
  private DuckDBEngine mockEngine;
  private DuckDBConnectionPool connectionPool;

  @BeforeEach
  void setUp() throws Exception {
    // Initialize DuckDB engine with connection pool
    EngineConfig engineConfig = mock(EngineConfig.class);
    when(engineConfig.databasePath()).thenReturn(":memory:");
    when(engineConfig.duckdb()).thenReturn(java.util.Optional.empty());

    ConnectionPoolConfig poolConfig = mock(ConnectionPoolConfig.class);
    when(poolConfig.maximumPoolSize()).thenReturn(2);
    when(poolConfig.minimumIdle()).thenReturn(0);
    when(poolConfig.connectionTimeout()).thenReturn(30000L);
    when(poolConfig.idleTimeout()).thenReturn(600000L);
    when(poolConfig.maxLifetime()).thenReturn(1800000L);
    when(poolConfig.poolName()).thenReturn("TestScheduledPool");

    connectionPool = new DuckDBConnectionPool();
    connectionPool.setEngineConfig(engineConfig);
    connectionPool.setPoolConfig(poolConfig);
    connectionPool.initialize();

    mockEngine = new DuckDBEngine();
    mockEngine.connectionPool = connectionPool;
    mockEngine.initialize();

    service = new ScheduledQueryService();
    // Inject mock engine
    var engineField = ScheduledQueryService.class.getDeclaredField("engine");
    engineField.setAccessible(true);
    engineField.set(service, mockEngine);
  }

  @AfterEach
  void tearDown() {
    if (connectionPool != null) {
      connectionPool.cleanup();
    }
  }

  @Test
  void testRegisterScheduledQuery() {
    service.registerScheduledQuery("test1", "SELECT 1", "0 */5 * * * ?");

    List<ScheduledQueryService.ScheduledQueryInfo> queries = service.getAllScheduledQueries();
    assertEquals(1, queries.size());
    assertEquals("test1", queries.get(0).queryId());
  }

  @Test
  void testUnregisterScheduledQuery() {
    service.registerScheduledQuery("test1", "SELECT 1", "0 */5 * * * ?");
    service.unregisterScheduledQuery("test1");

    List<ScheduledQueryService.ScheduledQueryInfo> queries = service.getAllScheduledQueries();
    assertEquals(0, queries.size());
  }

  @Test
  void testGetQueryStatus() {
    service.registerScheduledQuery("test1", "SELECT 1", "0 */5 * * * ?");

    List<ScheduledQueryService.ScheduledQueryInfo> queries = service.getAllScheduledQueries();
    assertEquals(1, queries.size());

    ScheduledQueryService.ScheduledQueryInfo info = queries.get(0);
    assertEquals("test1", info.queryId());
    assertEquals("SELECT 1", info.sql());
    assertEquals("0 */5 * * * ?", info.cronExpression());
  }

  @Test
  void testGetQueryStatusNotFound() {
    List<ScheduledQueryService.ScheduledQueryInfo> queries = service.getAllScheduledQueries();
    assertEquals(0, queries.size());
  }
}
