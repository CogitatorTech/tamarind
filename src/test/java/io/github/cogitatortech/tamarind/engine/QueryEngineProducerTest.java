package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.github.cogitatortech.tamarind.config.QueryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for QueryEngineProducer. */
class QueryEngineProducerTest {

  private QueryEngineProducer producer;

  @Mock private EngineConfig engineConfig;
  @Mock private QueryConfig queryConfig;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    producer = new QueryEngineProducer();
    producer.engineConfig = engineConfig;
    producer.queryConfig = queryConfig;
  }

  @Test
  void testProduceQueryEngine() {
    when(engineConfig.type()).thenReturn("duckdb");
    when(engineConfig.databasePath()).thenReturn(":memory:");

    QueryEngine engine = producer.produceQueryEngine();

    assertNotNull(engine);
    assertInstanceOf(DuckDBEngine.class, engine);
  }

  @Test
  void testProduceQueryEngineWithNullConfig() {
    producer.engineConfig = null;

    QueryEngine engine = producer.produceQueryEngine();

    assertNotNull(engine);
    // Should default to duckdb
  }

  @Test
  void testProduceQueryEngineDefaultType() {
    when(engineConfig.type()).thenReturn(null);

    QueryEngine engine = producer.produceQueryEngine();

    assertNotNull(engine);
  }

  @Test
  void testProduceQueryEngineCaseInsensitive() {
    when(engineConfig.type()).thenReturn("DUCKDB");
    when(engineConfig.databasePath()).thenReturn(":memory:");

    QueryEngine engine = producer.produceQueryEngine();

    assertNotNull(engine);
  }

  @Test
  void testProduceQueryEngineUnsupportedType() {
    when(engineConfig.type()).thenReturn("unsupported");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          producer.produceQueryEngine();
        });
  }

  @Test
  void testProducedEngineIsConfigured() {
    when(engineConfig.type()).thenReturn("duckdb");
    when(engineConfig.databasePath()).thenReturn(":memory:");

    QueryEngine engine = producer.produceQueryEngine();

    assertNotNull(engine);
    // Verify the engine is the correct type
    assertInstanceOf(DuckDBEngine.class, engine);

    // Verify configuration was set
    DuckDBEngine duckEngine = (DuckDBEngine) engine;
    assertEquals(engineConfig, duckEngine.engineConfig);
    assertEquals(queryConfig, duckEngine.queryConfig);
  }
}
