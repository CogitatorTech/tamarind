package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CdiEngineWiringTest {

  @Inject QueryEngine queryEngine;
  @Inject AnalyticalQueryEngine analyticalQueryEngine;
  @Inject JdbcQueryEngine jdbcQueryEngine;

  @Test
  void allInjectedBeansReferToSameUnderlyingDuckDBEngine() {
    assertNotNull(queryEngine);
    assertNotNull(analyticalQueryEngine);
    assertNotNull(jdbcQueryEngine);

    // They should all be the same @ApplicationScoped DuckDBEngine instance
    assertSame(
        queryEngine,
        analyticalQueryEngine,
        "QueryEngine and AnalyticalQueryEngine should be same instance");
    assertSame(
        queryEngine, jdbcQueryEngine, "QueryEngine and JdbcQueryEngine should be same instance");
    assertTrue(queryEngine instanceof DuckDBEngine, "Backed by DuckDBEngine implementation");
  }
}
