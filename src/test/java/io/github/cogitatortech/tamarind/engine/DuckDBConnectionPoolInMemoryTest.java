package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cogitatortech.tamarind.config.ConnectionPoolConfig;
import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DuckDBConnectionPoolInMemoryTest {

  @Inject DuckDBConnectionPool pool;
  @Inject EngineConfig engineConfig;
  @Inject ConnectionPoolConfig poolConfig;

  @Test
  void whenDatabasePathIsBlank_useInMemoryUrl() throws Exception {
    assertNotNull(pool, "Pool should be injected");
    assertTrue(pool.isHealthy(), "Pool should be healthy for in-memory DB");
  }
}
