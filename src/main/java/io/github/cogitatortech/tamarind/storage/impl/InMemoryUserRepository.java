package io.github.cogitatortech.tamarind.storage.impl;

import io.github.cogitatortech.tamarind.security.User;
import io.github.cogitatortech.tamarind.storage.UserRepository;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * In-memory implementation of UserRepository using ConcurrentHashMap.
 *
 * <p>This is a temporary default implementation that works without external dependencies. For
 * production, switch to JpaUserRepository with proper database configuration.
 *
 * <p>To enable JPA-based implementation: 1. Enable hibernate-orm in application.yml 2. Configure
 * datasource 3. Enable Flyway migrations 4. Create @Alternative JpaUserRepository with higher
 * priority
 */
@ApplicationScoped
@DefaultBean
public class InMemoryUserRepository implements UserRepository {

  private static final Logger LOGGER = LogManager.getLogger(InMemoryUserRepository.class);

  private final Map<String, User> users = new ConcurrentHashMap<>();
  private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

  public InMemoryUserRepository() {
    // Create default admin user with BCrypt hash for password "admin"
    // Generate hash at runtime to ensure it works correctly
    String adminPassword = "admin";
    String passwordHash =
        at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, adminPassword.toCharArray());

    User admin = new User("admin", "admin@tamarind.local", passwordHash);
    admin.setRole(io.github.cogitatortech.tamarind.security.Role.ADMIN);
    users.put(admin.getUsername(), admin);
    usersByEmail.put(admin.getEmail(), admin);
    LOGGER.info(
        "InMemoryUserRepository initialized with default admin user (username: admin, password: admin)");
    LOGGER.debug("Generated BCrypt hash: {}", passwordHash);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return Optional.ofNullable(users.get(username));
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return Optional.ofNullable(usersByEmail.get(email));
  }

  @Override
  public User save(User user) {
    users.put(user.getUsername(), user);
    usersByEmail.put(user.getEmail(), user);
    LOGGER.debug("Saved user: {}", user.getUsername());
    return user;
  }

  @Override
  public boolean delete(String username) {
    User user = users.remove(username);
    if (user != null) {
      usersByEmail.remove(user.getEmail());
      LOGGER.info("Deleted user: {}", username);
      return true;
    }
    return false;
  }

  @Override
  public List<User> findAll() {
    return new ArrayList<>(users.values());
  }

  @Override
  public long count() {
    return users.size();
  }

  @Override
  public boolean exists(String username) {
    return users.containsKey(username);
  }
}
