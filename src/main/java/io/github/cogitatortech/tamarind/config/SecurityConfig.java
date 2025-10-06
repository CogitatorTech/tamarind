package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "tamarind.security")
public interface SecurityConfig {

  RateLimitConfig rateLimit();

  AuditConfig audit();

  interface RateLimitConfig {
    @WithDefault("true")
    Boolean enabled();

    @WithDefault("100")
    Integer requestsPerMinute();

    @WithDefault("100")
    Integer capacity();

    @WithDefault("100")
    Integer refillTokens();

    @WithDefault("1")
    Integer refillPeriodMinutes();
  }

  interface AuditConfig {
    @WithDefault("true")
    Boolean enabled();
  }
}
