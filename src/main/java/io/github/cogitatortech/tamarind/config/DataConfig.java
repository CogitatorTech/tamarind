package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;

/** Configuration for data sources. */
@ConfigMapping(prefix = "tamarind.data")
public interface DataConfig {

  String source();
}
