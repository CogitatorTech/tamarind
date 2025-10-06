package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "tamarind.cache")
public interface CacheConfig {

  @WithDefault("5")
  Integer fileListTtlMinutes();

  @WithDefault("15")
  Integer schemaTtlMinutes();

  @WithDefault("10")
  Integer queryResultTtlMinutes();

  @WithDefault("500")
  Integer maxSize();
}
