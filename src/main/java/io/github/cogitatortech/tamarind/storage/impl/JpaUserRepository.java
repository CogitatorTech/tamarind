package io.github.cogitatortech.tamarind.storage.impl;

import io.github.cogitatortech.tamarind.security.User;
import io.github.cogitatortech.tamarind.storage.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JPA-based implementation of UserRepository.
 *
 * <p>This is an ALTERNATIVE implementation that requires database configuration. The default
 * implementation is InMemoryUserRepository.
 *
 * <p>To enable this JPA implementation: 1. Enable Hibernate ORM in application.yml 2. Configure
 * datasource 3. Enable Flyway migrations 4. Add to application.properties:
 * quarkus.arc.selected-alternatives=io.github.cogitatortech.tamarind.storage.impl.JpaUserRepository
 *
 * <p>Works with any JPA-compatible database: - SQLite (default) - PostgreSQL - MySQL - H2
 *
 * <p>Database selection is configured via application.yml:
 *
 * <pre>
 * quarkus:
 *   datasource:
 *     db-kind: sqlite  # or postgresql, mysql, h2
 * </pre>
 */
@ApplicationScoped
@Alternative
public class JpaUserRepository implements UserRepository {

  private static final Logger LOGGER = LogManager.getLogger(JpaUserRepository.class);

  @PersistenceContext EntityManager entityManager;

  @Override
  public Optional<User> findByUsername(String username) {
    try {
      User user =
          entityManager
              .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
              .setParameter("username", username)
              .getSingleResult();
      return Optional.of(user);
    } catch (jakarta.persistence.NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> findByEmail(String email) {
    try {
      User user =
          entityManager
              .createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
              .setParameter("email", email)
              .getSingleResult();
      return Optional.of(user);
    } catch (jakarta.persistence.NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  @Transactional
  public User save(User user) {
    if (entityManager.contains(user)) {
      User merged = entityManager.merge(user);
      LOGGER.debug("Updated user: {}", user.getUsername());
      return merged;
    } else {
      entityManager.persist(user);
      LOGGER.debug("Created user: {}", user.getUsername());
      return user;
    }
  }

  @Override
  @Transactional
  public boolean delete(String username) {
    Optional<User> user = findByUsername(username);
    if (user.isPresent()) {
      entityManager.remove(user.get());
      LOGGER.info("Deleted user: {}", username);
      return true;
    }
    return false;
  }

  @Override
  public List<User> findAll() {
    return entityManager
        .createQuery("SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
        .getResultList();
  }

  @Override
  public long count() {
    return entityManager.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
  }

  @Override
  public boolean exists(String username) {
    Long count =
        entityManager
            .createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
            .setParameter("username", username)
            .getSingleResult();
    return count > 0;
  }
}
