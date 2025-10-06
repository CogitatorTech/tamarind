package io.github.cogitatortech.tamarind.service.catalog;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IcebergCatalogService {
  public boolean isConfigured() {
    // Placeholder until real catalog wiring is added
    return false;
  }

  public boolean isHealthy() {
    return isConfigured();
  }
}
