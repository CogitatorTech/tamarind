package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

/** Configuration for query execution settings. */
@ConfigMapping(prefix = "tamarind.query")
public interface QueryConfig {

  /** Default query timeout in seconds. */
  Optional<Integer> timeoutSeconds();

  /** Maximum result rows to return. */
  Optional<Integer> maxResultRows();

  /** Enable query result caching. */
  Optional<Boolean> cachingEnabled();
}
