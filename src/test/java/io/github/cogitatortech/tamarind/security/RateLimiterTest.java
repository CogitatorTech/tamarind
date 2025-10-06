package io.github.cogitatortech.tamarind.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for RateLimiter. */
class RateLimiterTest {

  private RateLimiter rateLimiter;
  private SecurityConfig securityConfig;
  private SecurityConfig.RateLimitConfig rateLimitConfig;

  @BeforeEach
  void setUp() {
    rateLimiter = new RateLimiter();

    // Mock security config
    securityConfig = mock(SecurityConfig.class);
    rateLimitConfig = mock(SecurityConfig.RateLimitConfig.class);

    when(securityConfig.rateLimit()).thenReturn(rateLimitConfig);
    when(rateLimitConfig.enabled()).thenReturn(true);
    when(rateLimitConfig.capacity()).thenReturn(10);
    when(rateLimitConfig.refillTokens()).thenReturn(10);
    when(rateLimitConfig.refillPeriodMinutes()).thenReturn(1);

    rateLimiter.securityConfig = securityConfig;
  }

  @Test
  void testAllowRequestWithinLimit() {
    String userId = "user1";

    // First 10 requests should be allowed
    for (int i = 0; i < 10; i++) {
      assertTrue(rateLimiter.allowRequest(userId), "Request " + i + " should be allowed");
    }
  }

  @Test
  void testRateLimitExceeded() {
    String userId = "user2";

    // Consume all tokens
    for (int i = 0; i < 10; i++) {
      rateLimiter.allowRequest(userId);
    }

    // 11th request should be blocked
    assertFalse(rateLimiter.allowRequest(userId), "Request should be blocked");
  }

  @Test
  void testDifferentUsersHaveSeparateLimits() {
    String user1 = "user1";
    String user2 = "user2";

    // Exhaust user1's limit
    for (int i = 0; i < 10; i++) {
      rateLimiter.allowRequest(user1);
    }
    assertFalse(rateLimiter.allowRequest(user1), "User1 should be blocked");

    // User2 should still have full capacity
    assertTrue(rateLimiter.allowRequest(user2), "User2 should be allowed");
  }

  @Test
  void testResetUser() {
    String userId = "user3";

    // Exhaust limit
    for (int i = 0; i < 10; i++) {
      rateLimiter.allowRequest(userId);
    }
    assertFalse(rateLimiter.allowRequest(userId), "Should be blocked");

    // Reset
    rateLimiter.resetUser(userId);

    // Should work again
    assertTrue(rateLimiter.allowRequest(userId), "Should be allowed after reset");
  }

  @Test
  void testGetRemainingTokens() {
    String userId = "user4";

    // Initially should have full capacity
    assertEquals(10, rateLimiter.getRemainingTokens(userId));

    // Consume 3 tokens
    rateLimiter.allowRequest(userId);
    rateLimiter.allowRequest(userId);
    rateLimiter.allowRequest(userId);

    // Should have 7 remaining
    assertEquals(7, rateLimiter.getRemainingTokens(userId));
  }

  @Test
  void testRateLimitDisabled() {
    when(rateLimitConfig.enabled()).thenReturn(false);

    String userId = "user5";

    // Should allow unlimited requests
    for (int i = 0; i < 100; i++) {
      assertTrue(rateLimiter.allowRequest(userId), "All requests should be allowed when disabled");
    }
  }

  @Test
  void testCleanupInactiveBuckets() throws InterruptedException {
    String user1 = "user1";
    String user2 = "user2";

    // Create buckets for two users
    rateLimiter.allowRequest(user1);
    rateLimiter.allowRequest(user2);

    // Verify both exist
    assertTrue(rateLimiter.getStats().contains("Active users: 2"));

    // Note: In real scenario, cleanup runs every 10 minutes for buckets older than 1 hour
    // For testing, we manually trigger cleanup (in production it's @Scheduled)
    rateLimiter.cleanupInactiveBuckets();

    // Buckets should still exist (not old enough)
    assertTrue(rateLimiter.getStats().contains("Active users: 2"));
  }

  @Test
  void testGetStats() {
    rateLimiter.allowRequest("user1");
    rateLimiter.allowRequest("user2");
    rateLimiter.allowRequest("user3");

    String stats = rateLimiter.getStats();
    assertNotNull(stats);
    assertTrue(stats.contains("Active users: 3"));
    assertTrue(stats.contains("Cleanup threshold"));
  }

  @Test
  void testNullSecurityConfig() {
    rateLimiter.securityConfig = null;

    // Should allow all requests when config is null
    assertTrue(rateLimiter.allowRequest("anyUser"));
  }
}
