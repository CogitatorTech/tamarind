package io.github.cogitatortech.tamarind.storage;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for ephemeral data storage (caching, sessions, rate limiting).
 *
 * <p>This interface allows pluggable implementations for different storage backends: - In-memory
 * (ConcurrentHashMap) - default, no external dependencies - Redis - distributed, production-grade -
 * Memcached - alternative caching solution - Hazelcast - distributed in-memory data grid
 */
public interface EphemeralRepository {

  /**
   * Store a value with optional TTL.
   *
   * @param key the key
   * @param value the value
   * @param ttl time-to-live, null for no expiration
   */
  void set(String key, String value, Duration ttl);

  /**
   * Store a value without expiration.
   *
   * @param key the key
   * @param value the value
   */
  default void set(String key, String value) {
    set(key, value, null);
  }

  /**
   * Get a value by key.
   *
   * @param key the key
   * @return Optional containing the value if found and not expired
   */
  Optional<String> get(String key);

  /**
   * Delete a value by key.
   *
   * @param key the key
   * @return true if key existed and was deleted
   */
  boolean delete(String key);

  /**
   * Check if a key exists.
   *
   * @param key the key
   * @return true if key exists and is not expired
   */
  boolean exists(String key);

  /**
   * Get all keys matching a pattern.
   *
   * <p>Pattern syntax: - "*" matches any sequence of characters - "?" matches any single character
   * - "prefix:*" matches all keys starting with "prefix:"
   *
   * @param pattern the pattern to match
   * @return set of matching keys
   */
  Set<String> keys(String pattern);

  /**
   * Delete all keys matching a pattern.
   *
   * @param pattern the pattern to match
   * @return number of keys deleted
   */
  long deletePattern(String pattern);

  /**
   * Increment a numeric value atomically.
   *
   * @param key the key
   * @return the new value after increment
   */
  long increment(String key);

  /**
   * Increment a numeric value by a specific amount atomically.
   *
   * @param key the key
   * @param delta the amount to increment by
   * @return the new value after increment
   */
  long incrementBy(String key, long delta);

  /**
   * Decrement a numeric value atomically.
   *
   * @param key the key
   * @return the new value after decrement
   */
  long decrement(String key);

  /**
   * Set expiration time for a key.
   *
   * @param key the key
   * @param ttl time-to-live
   * @return true if expiration was set
   */
  boolean expire(String key, Duration ttl);

  /**
   * Get remaining time-to-live for a key.
   *
   * @param key the key
   * @return remaining TTL, empty if key doesn't exist or has no expiration
   */
  Optional<Duration> ttl(String key);

  /** Clear all data (use with caution). */
  void clear();

  /**
   * Get the storage backend name.
   *
   * @return name of the storage backend (e.g., "in-memory", "redis")
   */
  String getBackendName();
}
