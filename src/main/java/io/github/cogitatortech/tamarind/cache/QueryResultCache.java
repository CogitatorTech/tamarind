package io.github.cogitatortech.tamarind.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.cogitatortech.tamarind.config.CacheConfig;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Query result cache to improve performance for repeated queries. Uses normalized SQL hash as key
 * and caches the result.
 */
@ApplicationScoped
public class QueryResultCache {

  private static final Logger LOGGER = LogManager.getLogger(QueryResultCache.class);

  @Inject CacheConfig cacheConfig;

  private Cache<String, QueryResult> cache;

  @Inject
  public void initialize() {
    int ttl = cacheConfig != null ? cacheConfig.queryResultTtlMinutes() : 10;
    int maxSize = cacheConfig != null ? cacheConfig.maxSize() : 100;

    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(ttl))
            .maximumSize(maxSize)
            .recordStats()
            .build();

    LOGGER.info("Query result cache initialized (TTL: {}m, MaxSize: {})", ttl, maxSize);
  }

  /**
   * Get cached query result using normalized SQL.
   *
   * @param sql The SQL query
   * @return Cached result or null
   */
  public QueryResult get(String sql) {
    if (sql == null) {
      return null;
    }
    if (cache == null) {
      return null;
    }
    String cacheKey = SqlNormalizer.generateResultCacheKey(sql);
    QueryResult result = cache.getIfPresent(cacheKey);
    if (result != null) {
      LOGGER.debug("Cache hit for query: {}", truncate(sql));
    } else {
      LOGGER.debug("Cache miss for query: {}", truncate(sql));
    }
    return result;
  }

  /**
   * Cache a query result using normalized SQL.
   *
   * @param sql The SQL query
   * @param result The query result
   */
  public void put(String sql, QueryResult result) {
    if (sql == null || result == null) {
      return;
    }
    if (cache == null) {
      LOGGER.warn("Cache not initialized, skipping put");
      return;
    }
    String cacheKey = SqlNormalizer.generateResultCacheKey(sql);
    cache.put(cacheKey, result);
    LOGGER.debug("Cached result for query: {}", truncate(sql));
  }

  /** Invalidate all cached results. */
  public void invalidateAll() {
    if (cache != null) {
      cache.invalidateAll();
      LOGGER.info("All query results invalidated");
    }
  }

  /** Get cache statistics. */
  public String getStats() {
    if (cache == null) {
      return "Cache not initialized";
    }
    return cache.stats().toString();
  }

  /**
   * Get cache hit rate for monitoring.
   *
   * @return Hit rate as percentage (0.0 to 1.0)
   */
  public double getHitRate() {
    if (cache == null) {
      return 0.0;
    }
    return cache.stats().hitRate();
  }

  private String truncate(String sql) {
    if (sql == null) {
      return "";
    }
    return sql.length() > 50 ? sql.substring(0, 47) + "..." : sql;
  }
}
