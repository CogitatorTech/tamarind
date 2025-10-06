package io.github.cogitatortech.tamarind.service.storage;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class S3HealthProbe {
  public boolean isReachable() {
    // Placeholder: in the future, try listing a known bucket/prefix when configured.
    return true;
  }
}
