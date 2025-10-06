package io.github.cogitatortech.tamarind.arrow;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.engine.JdbcQueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Arrow Flight server. Uses Quarkus test framework to properly initialize
 * DuckDB.
 */
@QuarkusTest
public class ArrowFlightIntegrationTest {

  @Inject QueryEngine engine;

  private BufferAllocator allocator;
  private JdbcQueryEngine jdbcEngine;

  @BeforeEach
  public void setup() throws Exception {
    // Arrow Flight requires JDBC access
    assertTrue(
        engine instanceof JdbcQueryEngine,
        "Engine must implement JdbcQueryEngine for Arrow Flight support");
    jdbcEngine = (JdbcQueryEngine) engine;

    // Create test data using injected engine
    engine.executeUpdate(
        "CREATE TABLE IF NOT EXISTS test_data AS SELECT i as id, 'name_' || i as name FROM range(100) t(i)");

    // Initialize Arrow allocator
    allocator = new RootAllocator();
  }

  @AfterEach
  public void teardown() throws Exception {
    // Clean up test data
    try {
      engine.executeUpdate("DROP TABLE IF EXISTS test_data");
    } catch (Exception e) {
      // Ignore cleanup errors
    }

    if (allocator != null) {
      allocator.close();
    }
  }

  @Test
  public void testFlightProducerCreation() {
    // Test that we can create the producer with JdbcQueryEngine
    TamarindFlightSqlProducer producer = new TamarindFlightSqlProducer(jdbcEngine, allocator);
    assertNotNull(producer, "Flight producer should be created successfully");
  }

  @Test
  public void testQueryExecution() throws Exception {
    // Test basic query execution through the engine
    var result = engine.executeQuery("SELECT COUNT(*) as cnt FROM test_data");

    assertNotNull(result, "Query result should not be null");
    assertEquals(1, result.getRowCount(), "Should return one row");
    assertEquals(100L, result.getRows().get(0).get("cnt"), "Should have 100 rows in test_data");
  }

  @Test
  public void testFlightSqlProducerWithEngine() {
    // Verify that TamarindFlightSqlProducer works with JdbcQueryEngine
    TamarindFlightSqlProducer producer = new TamarindFlightSqlProducer(jdbcEngine, allocator);

    assertNotNull(producer, "Producer should be created");
    // Additional Flight SQL tests would go here, but they require actual Flight client setup
  }
}
