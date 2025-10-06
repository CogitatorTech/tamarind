package io.github.cogitatortech.tamarind.security;

/** User roles for role-based access control. */
public enum Role {
  /** Administrator with full system access */
  ADMIN,

  /** Regular user with query and read access */
  USER,

  /** Read-only viewer with limited access */
  VIEWER;

  public static Role fromString(String role) {
    try {
      return Role.valueOf(role.toUpperCase());
    } catch (IllegalArgumentException e) {
      return USER; // Default to USER role
    }
  }

  public String getName() {
    return name();
  }
}
