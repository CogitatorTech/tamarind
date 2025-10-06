package io.github.cogitatortech.tamarind.service;

import io.github.cogitatortech.tamarind.cache.QueryResultCache;
import io.github.cogitatortech.tamarind.engine.DuckDBConnectionPool;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.service.catalog.IcebergCatalogService;
import io.github.cogitatortech.tamarind.service.storage.S3HealthProbe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

/** Health checks for the Tamarind application. */
@ApplicationScoped
public class TamarindHealthChecks {

  @Inject QueryEngine engine;

  @Inject DuckDBConnectionPool connectionPool;

  @Inject QueryResultCache queryCache;

  @Inject io.github.cogitatortech.tamarind.security.RateLimiter rateLimiter;

  @Inject IcebergCatalogService icebergCatalogService;
  @Inject S3HealthProbe s3HealthProbe;

  /** Liveness probe - checks if the application is running. */
  @Liveness
  public HealthCheckResponse liveness() {
    return HealthCheckResponse.named("tamarind-liveness").up().build();
  }

  /** Readiness probe - checks if the application is ready to serve requests. */
  @Readiness
  public HealthCheckResponse readiness() {
    return HealthCheckResponse.named("tamarind-readiness").status(engine.isHealthy()).build();
  }

  @Readiness
  public HealthCheckResponse connectionPoolHealth() {
    boolean healthy = connectionPool.isHealthy();

    if (healthy) {
      return HealthCheckResponse.named("connection-pool")
          .up()
          .withData("stats", connectionPool.getStats())
          .build();
    } else {
      return HealthCheckResponse.named("connection-pool")
          .down()
          .withData("error", "Connection pool is not healthy")
          .build();
    }
  }

  @Readiness
  public HealthCheckResponse cacheHealth() {
    double hitRate = queryCache.getHitRate();
    return HealthCheckResponse.named("query-cache")
        .up()
        .withData("hitRate", String.format("%.2f%%", hitRate * 100))
        .withData("stats", queryCache.getStats())
        .build();
  }

  @Readiness
  public HealthCheckResponse rateLimiterHealth() {
    String stats = rateLimiter.getStats();
    return HealthCheckResponse.named("rate-limiter").up().withData("stats", stats).build();
  }

  @Readiness
  public HealthCheckResponse s3Connectivity() {
    boolean ok = s3HealthProbe.isReachable();
    return HealthCheckResponse.named("s3-connectivity").status(ok).build();
  }

  @Readiness
  public HealthCheckResponse icebergCatalog() {
    boolean ok = icebergCatalogService.isConfigured() && icebergCatalogService.isHealthy();
    return HealthCheckResponse.named("iceberg-catalog").status(ok).build();
  }
}
