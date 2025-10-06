package io.github.cogitatortech.tamarind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.cache.QueryResultCache;
import io.github.cogitatortech.tamarind.engine.DuckDBConnectionPool;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for TamarindHealthChecks. */
class TamarindHealthChecksTest {

  private TamarindHealthChecks healthChecks;

  @Mock private QueryEngine engine;
  @Mock private DuckDBConnectionPool connectionPool;
  @Mock private QueryResultCache queryCache;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    healthChecks = new TamarindHealthChecks();
    healthChecks.engine = engine;
    healthChecks.connectionPool = connectionPool;
    healthChecks.queryCache = queryCache;
  }

  @Test
  void testLivenessCheckAlwaysUp() {
    HealthCheckResponse response = healthChecks.liveness();

    assertNotNull(response);
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("tamarind-liveness", response.getName());
  }

  @Test
  void testReadinessCheckWhenEngineHealthy() {
    when(engine.isHealthy()).thenReturn(true);

    HealthCheckResponse response = healthChecks.readiness();

    assertNotNull(response);
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("tamarind-readiness", response.getName());
  }

  @Test
  void testReadinessCheckWhenEngineUnhealthy() {
    when(engine.isHealthy()).thenReturn(false);

    HealthCheckResponse response = healthChecks.readiness();

    assertNotNull(response);
    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    assertEquals("tamarind-readiness", response.getName());
  }

  @Test
  void testConnectionPoolHealthWhenHealthy() {
    when(connectionPool.isHealthy()).thenReturn(true);
    when(connectionPool.getStats()).thenReturn("Active: 1, Idle: 2, Total: 3, Waiting: 0");

    HealthCheckResponse response = healthChecks.connectionPoolHealth();

    assertNotNull(response);
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("connection-pool", response.getName());
  }

  @Test
  void testConnectionPoolHealthWhenUnhealthy() {
    when(connectionPool.isHealthy()).thenReturn(false);

    HealthCheckResponse response = healthChecks.connectionPoolHealth();

    assertNotNull(response);
    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    assertEquals("connection-pool", response.getName());
  }

  @Test
  void testCacheHealth() {
    when(queryCache.getHitRate()).thenReturn(0.75);
    when(queryCache.getStats()).thenReturn("Size: 100, Hits: 75, Misses: 25");

    HealthCheckResponse response = healthChecks.cacheHealth();

    assertNotNull(response);
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("query-cache", response.getName());
  }
}
