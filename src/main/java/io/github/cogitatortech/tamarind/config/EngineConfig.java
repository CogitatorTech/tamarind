package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

@ConfigMapping(prefix = "tamarind.engine")
public interface EngineConfig {

  @WithDefault("duckdb")
  String type();

  @WithDefault(":memory:")
  String databasePath();

  Optional<DuckDBConfig> duckdb();

  interface DuckDBConfig {
    @WithDefault("read_write")
    String accessMode();

    @WithDefault("4")
    Integer threads();

    @WithDefault("true")
    Boolean installHttpfs();

    @WithDefault("true")
    Boolean loadHttpfs();
  }
}
