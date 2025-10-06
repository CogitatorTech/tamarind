package io.github.cogitatortech.tamarind.storage.impl;

import io.github.cogitatortech.tamarind.storage.EphemeralRepository;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * In-memory implementation of EphemeralRepository using ConcurrentHashMap.
 *
 * <p>This is the default implementation, requiring no external dependencies. Suitable for: -
 * Development and testing - Single-instance deployments - Small-scale applications
 *
 * <p>Limitations: - Data lost on restart - Not shared across multiple instances - Memory-bound
 * capacity
 */
@ApplicationScoped
@DefaultBean
public class InMemoryEphemeralRepository implements EphemeralRepository {

  private static final Logger LOGGER = LogManager.getLogger(InMemoryEphemeralRepository.class);

  private final Map<String, Entry> storage = new ConcurrentHashMap<>();

  @Override
  public void set(String key, String value, Duration ttl) {
    Instant expiresAt = ttl != null ? Instant.now().plus(ttl) : null;
    storage.put(key, new Entry(value, expiresAt));
    LOGGER.debug("Set key: {}, TTL: {}", key, ttl);
  }

  @Override
  public Optional<String> get(String key) {
    Entry entry = storage.get(key);
    if (entry == null) {
      return Optional.empty();
    }

    if (entry.isExpired()) {
      storage.remove(key);
      LOGGER.debug("Key expired and removed: {}", key);
      return Optional.empty();
    }

    return Optional.of(entry.value);
  }

  @Override
  public boolean delete(String key) {
    boolean existed = storage.remove(key) != null;
    if (existed) {
      LOGGER.debug("Deleted key: {}", key);
    }
    return existed;
  }

  @Override
  public boolean exists(String key) {
    Entry entry = storage.get(key);
    if (entry == null) {
      return false;
    }

    if (entry.isExpired()) {
      storage.remove(key);
      return false;
    }

    return true;
  }

  @Override
  public Set<String> keys(String pattern) {
    // Convert glob pattern to regex
    String regex = globToRegex(pattern);
    Pattern p = Pattern.compile(regex);

    return storage.keySet().stream()
        .filter(key -> p.matcher(key).matches())
        .filter(key -> !storage.get(key).isExpired())
        .collect(Collectors.toSet());
  }

  @Override
  public long deletePattern(String pattern) {
    Set<String> keysToDelete = keys(pattern);
    keysToDelete.forEach(storage::remove);
    LOGGER.debug("Deleted {} keys matching pattern: {}", keysToDelete.size(), pattern);
    return keysToDelete.size();
  }

  @Override
  public long increment(String key) {
    return incrementBy(key, 1);
  }

  @Override
  public long incrementBy(String key, long delta) {
    return storage
        .compute(
            key,
            (k, entry) -> {
              if (entry == null || entry.isExpired()) {
                return new Entry(String.valueOf(delta), null);
              }

              try {
                long currentValue = Long.parseLong(entry.value);
                long newValue = currentValue + delta;
                return new Entry(String.valueOf(newValue), entry.expiresAt);
              } catch (NumberFormatException e) {
                LOGGER.warn("Cannot increment non-numeric value for key: {}", key);
                throw new IllegalStateException("Value is not a number");
              }
            })
        .getValueAsLong();
  }

  @Override
  public long decrement(String key) {
    return incrementBy(key, -1);
  }

  @Override
  public boolean expire(String key, Duration ttl) {
    Entry entry = storage.get(key);
    if (entry == null || entry.isExpired()) {
      return false;
    }

    Instant newExpiry = Instant.now().plus(ttl);
    storage.put(key, new Entry(entry.value, newExpiry));
    return true;
  }

  @Override
  public Optional<Duration> ttl(String key) {
    Entry entry = storage.get(key);
    if (entry == null || entry.expiresAt == null) {
      return Optional.empty();
    }

    if (entry.isExpired()) {
      storage.remove(key);
      return Optional.empty();
    }

    Duration remaining = Duration.between(Instant.now(), entry.expiresAt);
    return Optional.of(remaining);
  }

  @Override
  public void clear() {
    int size = storage.size();
    storage.clear();
    LOGGER.info("Cleared all {} entries from in-memory storage", size);
  }

  @Override
  public String getBackendName() {
    return "in-memory";
  }

  /**
   * Cleanup expired entries periodically. This prevents memory leaks from expired but not accessed
   * entries.
   */
  public void cleanupExpired() {
    Instant now = Instant.now();
    List<String> expiredKeys =
        storage.entrySet().stream()
            .filter(e -> e.getValue().expiresAt != null && e.getValue().expiresAt.isBefore(now))
            .map(Map.Entry::getKey)
            .toList();

    expiredKeys.forEach(storage::remove);

    if (!expiredKeys.isEmpty()) {
      LOGGER.debug("Cleaned up {} expired entries", expiredKeys.size());
    }
  }

  /** Convert glob pattern to regex. */
  private String globToRegex(String glob) {
    StringBuilder regex = new StringBuilder("^");
    for (char c : glob.toCharArray()) {
      switch (c) {
        case '*':
          regex.append(".*");
          break;
        case '?':
          regex.append(".");
          break;
        case '.':
        case '(':
        case ')':
        case '[':
        case ']':
        case '$':
        case '^':
        case '|':
        case '+':
        case '\\':
          regex.append("\\").append(c);
          break;
        default:
          regex.append(c);
      }
    }
    regex.append("$");
    return regex.toString();
  }

  /** Internal storage entry with optional expiration. */
  private static class Entry {
    final String value;
    final Instant expiresAt;

    Entry(String value, Instant expiresAt) {
      this.value = value;
      this.expiresAt = expiresAt;
    }

    boolean isExpired() {
      return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    long getValueAsLong() {
      return Long.parseLong(value);
    }
  }
}
