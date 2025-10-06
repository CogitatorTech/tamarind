package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "tamarind.connection-pool")
public interface ConnectionPoolConfig {

  @WithDefault("10")
  Integer maximumPoolSize();

  @WithDefault("2")
  Integer minimumIdle();

  @WithDefault("30000")
  Long connectionTimeout();

  @WithDefault("600000")
  Long idleTimeout();

  @WithDefault("1800000")
  Long maxLifetime();

  @WithDefault("DuckDBPool")
  String poolName();
}
