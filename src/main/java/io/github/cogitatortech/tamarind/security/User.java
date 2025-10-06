package io.github.cogitatortech.tamarind.security;

import java.time.Instant;

/**
 * User entity for authentication and authorization.
 *
 * <p>Can be used with or without JPA: - With JPA: Add @Entity, @Table, @Id, @Column annotations -
 * Without JPA: Works as plain POJO
 *
 * <p>For JPA persistence, uncomment the imports and annotations below.
 */
// Uncomment for JPA:
// import jakarta.persistence.*;

// @Entity
// @Table(name = "users")
public class User {

  // @Id
  // @Column(length = 255, nullable = false)
  private String username;

  // @Column(nullable = false, unique = true, length = 255)
  private String email;

  // @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  // @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  // @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // @Column(nullable = false)
  private Boolean enabled = true;

  // @Column(name = "last_login")
  private Instant lastLogin;

  // @Column(length = 50, nullable = false)
  private Role role = Role.USER;

  public User() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public User(String username, String email, String passwordHash) {
    this();
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
  }

  // @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }

  // Getters and setters
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(Instant lastLogin) {
    this.lastLogin = lastLogin;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
