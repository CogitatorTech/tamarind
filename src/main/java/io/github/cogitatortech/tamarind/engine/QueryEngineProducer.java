package io.github.cogitatortech.tamarind.engine;

import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.github.cogitatortech.tamarind.config.QueryConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class QueryEngineProducer {

  private static final Logger LOGGER = LogManager.getLogger(QueryEngineProducer.class);

  @Inject EngineConfig engineConfig;
  @Inject QueryConfig queryConfig;
  @Inject DuckDBConnectionPool connectionPool;

  @Produces
  @ApplicationScoped
  @Typed({QueryEngine.class, JdbcQueryEngine.class, AnalyticalQueryEngine.class})
  public DuckDBEngine produceQueryEngine() {
    String engineType =
        engineConfig != null && engineConfig.type() != null ? engineConfig.type() : "duckdb";

    LOGGER.info("Initializing query engine: {}", engineType);

    switch (engineType.toLowerCase()) {
      case "duckdb":
        DuckDBEngine engine = new DuckDBEngine();
        engine.engineConfig = engineConfig;
        engine.queryConfig = queryConfig;
        engine.connectionPool = connectionPool;
        engine.initialize();
        LOGGER.info("DuckDB engine initialized successfully");
        return engine;
      default:
        throw new IllegalArgumentException(
            "Unsupported engine type: " + engineType + ". Supported engines: duckdb");
    }
  }
}
