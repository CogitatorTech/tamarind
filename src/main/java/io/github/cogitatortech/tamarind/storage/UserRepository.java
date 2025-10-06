package io.github.cogitatortech.tamarind.storage;

import io.github.cogitatortech.tamarind.security.User;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for user persistence operations.
 *
 * <p>This interface allows pluggable implementations for different storage backends (SQLite,
 * PostgreSQL, MySQL, etc.)
 */
public interface UserRepository {

  /**
   * Find a user by username.
   *
   * @param username the username to search for
   * @return Optional containing the user if found, empty otherwise
   */
  Optional<User> findByUsername(String username);

  /**
   * Find a user by email.
   *
   * @param email the email to search for
   * @return Optional containing the user if found, empty otherwise
   */
  Optional<User> findByEmail(String email);

  /**
   * Save or update a user.
   *
   * @param user the user to persist
   * @return the persisted user
   */
  User save(User user);

  /**
   * Delete a user by username.
   *
   * @param username the username of the user to delete
   * @return true if user was deleted, false if not found
   */
  boolean delete(String username);

  /**
   * Get all users.
   *
   * @return list of all users
   */
  List<User> findAll();

  /**
   * Count total number of users.
   *
   * @return user count
   */
  long count();

  /**
   * Check if a user exists by username.
   *
   * @param username the username to check
   * @return true if user exists
   */
  boolean exists(String username);
}
