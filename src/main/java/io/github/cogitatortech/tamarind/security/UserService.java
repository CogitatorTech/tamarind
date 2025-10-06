package io.github.cogitatortech.tamarind.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.github.cogitatortech.tamarind.storage.EphemeralRepository;
import io.github.cogitatortech.tamarind.storage.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * User management service for authentication using pluggable storage backends.
 *
 * <p>Storage: - User accounts: Persistent OLTP database (SQLite default, PostgreSQL optional) -
 * Auth tokens: Ephemeral storage (in-memory default, Redis optional)
 *
 * <p>Security: Uses BCrypt for password hashing with cost factor 12
 */
@ApplicationScoped
public class UserService {

  private static final Logger LOGGER = LogManager.getLogger(UserService.class);
  private static final Duration TOKEN_TTL = Duration.ofHours(24);
  private static final String TOKEN_PREFIX = "token:";
  private static final int BCRYPT_COST = 12; // BCrypt work factor (higher = more secure but slower)

  @Inject UserRepository userRepository;
  @Inject EphemeralRepository ephemeralRepository;

  /** Create a new user. */
  @Transactional
  public User createUser(String username, String email, String password) {
    LOGGER.debug("Creating user: {}", username);

    if (userRepository.exists(username)) {
      throw new IllegalArgumentException("Username already exists");
    }

    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalArgumentException("Email already exists");
    }

    String passwordHash = hashPassword(password);
    User user = new User(username, email, passwordHash);
    user = userRepository.save(user);

    LOGGER.info("User created: {}", username);
    return user;
  }

  /** Authenticate a user. */
  @Transactional
  public User authenticate(String username, String password) {
    LOGGER.debug("Authenticating user: {}", username);

    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      LOGGER.warn("User not found: {}", username);
      return null;
    }

    User user = userOpt.get();
    if (!user.getEnabled()) {
      LOGGER.warn("User disabled: {}", username);
      return null;
    }

    if (!verifyPassword(password, user.getPasswordHash())) {
      LOGGER.warn("Invalid password for user: {}", username);
      return null;
    }

    // Update last login
    user.setLastLogin(Instant.now());
    userRepository.save(user);

    LOGGER.info("Authentication successful: {}", username);
    return user;
  }

  /** Generate a token for a user. */
  public String generateToken(User user) {
    String token = UUID.randomUUID().toString();
    String tokenKey = TOKEN_PREFIX + token;

    // Store token -> username mapping in ephemeral storage
    ephemeralRepository.set(tokenKey, user.getUsername(), TOKEN_TTL);

    LOGGER.info(
        "Token generated for user: {} (backend: {})",
        user.getUsername(),
        ephemeralRepository.getBackendName());
    return token;
  }

  /** Validate a token. */
  public boolean validateToken(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }

    String tokenKey = TOKEN_PREFIX + token;
    boolean isValid = ephemeralRepository.exists(tokenKey);

    LOGGER.debug(
        "Token validation: {}, valid: {}",
        token.substring(0, Math.min(8, token.length())),
        isValid);
    return isValid;
  }

  /** Get username from token. */
  public String getUsernameFromToken(String token) {
    if (token == null) {
      return null;
    }

    String tokenKey = TOKEN_PREFIX + token;
    return ephemeralRepository.get(tokenKey).orElse(null);
  }

  /** Revoke a token. */
  public boolean revokeToken(String token) {
    if (token == null) {
      return false;
    }

    String tokenKey = TOKEN_PREFIX + token;
    boolean deleted = ephemeralRepository.delete(tokenKey);
    if (deleted) {
      LOGGER.info("Token revoked: {}...", token.substring(0, Math.min(8, token.length())));
    }
    return deleted;
  }

  /** Get all users. */
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  /** Get user by username. */
  public Optional<User> getUser(String username) {
    return userRepository.findByUsername(username);
  }

  /** Delete a user. */
  @Transactional
  public boolean deleteUser(String username) {
    boolean deleted = userRepository.delete(username);
    if (deleted) {
      LOGGER.info("User deleted: {}", username);
    }
    return deleted;
  }

  /** Count total users. */
  public long countUsers() {
    return userRepository.count();
  }

  /** Update user role (admin only). */
  @Transactional
  public boolean updateUserRole(String username, Role role) {
    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      return false;
    }

    User user = userOpt.get();
    user.setRole(role);
    userRepository.save(user);
    LOGGER.info("User role updated: {} -> {}", username, role);
    return true;
  }

  /** Check if user has specific role. */
  public boolean hasRole(String username, Role role) {
    return userRepository
        .findByUsername(username)
        .map(user -> user.getRole() == role)
        .orElse(false);
  }

  /** Check if user is admin. */
  public boolean isAdmin(String username) {
    return hasRole(username, Role.ADMIN);
  }

  /** Hash password using BCrypt with cost factor 12. BCrypt automatically handles salting. */
  private String hashPassword(String password) {
    return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
  }

  /** Verify password against BCrypt hash. */
  private boolean verifyPassword(String password, String hash) {
    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
    return result.verified;
  }
}
