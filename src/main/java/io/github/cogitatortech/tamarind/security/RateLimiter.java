package io.github.cogitatortech.tamarind.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.cogitatortech.tamarind.config.SecurityConfig;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Rate limiter service using token bucket algorithm. Tracks requests per user and automatically
 * cleans up inactive users to prevent memory leaks.
 */
@ApplicationScoped
public class RateLimiter {

  private static final Logger LOGGER = LogManager.getLogger(RateLimiter.class);
  private static final Duration CLEANUP_THRESHOLD = Duration.ofHours(1);

  private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

  @Inject SecurityConfig securityConfig;

  /**
   * Check if a request is allowed for the given user.
   *
   * @param userId The user identifier
   * @return true if request is allowed, false if rate limit exceeded
   */
  public boolean allowRequest(String userId) {
    if (securityConfig == null || !securityConfig.rateLimit().enabled()) {
      return true;
    }

    BucketEntry entry = buckets.computeIfAbsent(userId, this::createBucketEntry);
    entry.updateAccess();

    boolean allowed = entry.bucket.tryConsume(1);

    if (!allowed) {
      LOGGER.warn("Rate limit exceeded for user: {}", userId);
    }

    return allowed;
  }

  private BucketEntry createBucketEntry(String userId) {
    LOGGER.debug("Creating rate limit bucket for user: {}", userId);

    SecurityConfig.RateLimitConfig config = securityConfig.rateLimit();
    int capacity = config.capacity();
    int refillTokens = config.refillTokens();
    Duration refillPeriod = Duration.ofMinutes(config.refillPeriodMinutes());

    Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillTokens, refillPeriod));
    Bucket bucket = Bucket.builder().addLimit(limit).build();

    return new BucketEntry(bucket);
  }

  /**
   * Scheduled cleanup of inactive user buckets to prevent memory leaks. Runs every 10 minutes and
   * removes buckets that haven't been accessed in the last hour.
   */
  @Scheduled(every = "10m")
  void cleanupInactiveBuckets() {
    if (buckets.isEmpty()) {
      return;
    }

    Instant threshold = Instant.now().minus(CLEANUP_THRESHOLD);
    int beforeSize = buckets.size();

    buckets
        .entrySet()
        .removeIf(
            entry -> {
              Instant lastAccess = entry.getValue().lastAccess;
              boolean shouldRemove = lastAccess.isBefore(threshold);

              if (shouldRemove) {
                LOGGER.debug(
                    "Removing inactive bucket for user: {} (last access: {})",
                    entry.getKey(),
                    lastAccess);
              }

              return shouldRemove;
            });

    int removed = beforeSize - buckets.size();
    if (removed > 0) {
      LOGGER.info(
          "Cleaned up {} inactive rate limit buckets (active: {})", removed, buckets.size());
    }
  }

  /**
   * Reset rate limit for a specific user.
   *
   * @param userId The user identifier
   */
  public void resetUser(String userId) {
    buckets.remove(userId);
    LOGGER.info("Rate limit reset for user: {}", userId);
  }

  /**
   * Get remaining tokens for a user.
   *
   * @param userId The user identifier
   * @return Number of available tokens
   */
  public long getRemainingTokens(String userId) {
    BucketEntry entry = buckets.get(userId);
    if (entry != null) {
      return entry.bucket.getAvailableTokens();
    }
    return securityConfig != null ? securityConfig.rateLimit().capacity() : 100;
  }

  /**
   * Get statistics about rate limiting.
   *
   * @return Statistics string
   */
  public String getStats() {
    return String.format(
        "Active users: %d, Cleanup threshold: %s", buckets.size(), CLEANUP_THRESHOLD);
  }

  /** Entry containing bucket and last access time for cleanup purposes. */
  private static class BucketEntry {
    final Bucket bucket;
    volatile Instant lastAccess;

    BucketEntry(Bucket bucket) {
      this.bucket = bucket;
      this.lastAccess = Instant.now();
    }

    void updateAccess() {
      this.lastAccess = Instant.now();
    }
  }
}
