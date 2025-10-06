package io.github.cogitatortech.tamarind.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.cogitatortech.tamarind.config.CacheConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Metadata cache service using Caffeine. Caches S3 file listings and table schemas to improve
 * performance.
 */
@ApplicationScoped
public class MetadataCache {

  private static final Logger LOGGER = LogManager.getLogger(MetadataCache.class);

  @Inject CacheConfig cacheConfig;

  private Cache<String, List<String>> fileListCache;
  private Cache<String, String> schemaCache;

  @Inject
  public void initialize() {
    int fileListTtl = cacheConfig != null ? cacheConfig.fileListTtlMinutes() : 5;
    int schemaTtl = cacheConfig != null ? cacheConfig.schemaTtlMinutes() : 15;
    int maxSize = cacheConfig != null ? cacheConfig.maxSize() : 500;

    this.fileListCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(fileListTtl))
            .maximumSize(maxSize / 5)
            .recordStats()
            .build();

    this.schemaCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(schemaTtl))
            .maximumSize(maxSize)
            .recordStats()
            .build();

    LOGGER.info("Metadata cache initialized");
  }

  /**
   * Get cached file list for a path.
   *
   * @param path The S3 or local path
   * @return Cached file list or null if not cached
   */
  public List<String> getFileList(String path) {
    List<String> cached = fileListCache.getIfPresent(path);
    if (cached != null) {
      LOGGER.debug("File list cache hit for path: {}", path);
    }
    return cached;
  }

  /**
   * Cache file list for a path.
   *
   * @param path The S3 or local path
   * @param files List of files
   */
  public void putFileList(String path, List<String> files) {
    fileListCache.put(path, files);
    LOGGER.debug("Cached file list for path: {}", path);
  }

  /**
   * Get cached schema for a table.
   *
   * @param tableName The table name
   * @return Cached schema DDL or null if not cached
   */
  public String getSchema(String tableName) {
    String cached = schemaCache.getIfPresent(tableName);
    if (cached != null) {
      LOGGER.debug("Schema cache hit for table: {}", tableName);
    }
    return cached;
  }

  /**
   * Cache schema for a table.
   *
   * @param tableName The table name
   * @param schemaDdl The schema DDL
   */
  public void putSchema(String tableName, String schemaDdl) {
    schemaCache.put(tableName, schemaDdl);
    LOGGER.debug("Cached schema for table: {}", tableName);
  }

  /** Clear file list cache. */
  public void clearFileListCache() {
    fileListCache.invalidateAll();
    LOGGER.info("File list cache cleared");
  }

  /** Clear schema cache. */
  public void clearSchemaCache() {
    schemaCache.invalidateAll();
    LOGGER.info("Schema cache cleared");
  }

  /**
   * Get cache statistics.
   *
   * @return Cache stats as a formatted string
   */
  public CacheStats getStats() {
    return new CacheStats(
        fileListCache.estimatedSize(),
        schemaCache.estimatedSize(),
        fileListCache.stats().hitRate(),
        schemaCache.stats().hitRate());
  }

  public record CacheStats(
      long fileListSize, long schemaSize, double fileListHitRate, double schemaHitRate) {}
}
