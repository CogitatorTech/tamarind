package io.github.cogitatortech.tamarind.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

@ConfigMapping(prefix = "tamarind.arrow")
public interface ArrowConfig {

  @WithDefault("true")
  Boolean enabled();

  @WithDefault("0.0.0.0")
  String host();

  @WithDefault("8815")
  Integer port();

  @WithDefault("9223372036854775807")
  Long allocatorMaxMemory();

  TlsConfig tls();

  interface TlsConfig {
    @WithDefault("false")
    Boolean enabled();

    /** Path to server certificate chain (PEM). Optional when TLS disabled. */
    Optional<String> certFile();

    /** Path to server private key (PEM). Optional when TLS disabled. */
    Optional<String> keyFile();
  }
}
