package io.github.cogitatortech.tamarind.security;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Ensures UserService is initialized at application startup. */
@ApplicationScoped
public class SecurityInitializer {

  private static final Logger LOGGER = LogManager.getLogger(SecurityInitializer.class);

  @Inject UserService userService;

  void onStart(@Observes StartupEvent event) {
    LOGGER.info("SecurityInitializer: Forcing UserService initialization...");
    // Just accessing the userService will trigger its @PostConstruct
    // Let's also explicitly call a method to ensure it's fully initialized
    int userCount = userService.getAllUsers().size();
    LOGGER.info("SecurityInitializer: UserService initialized with {} users", userCount);
  }
}
