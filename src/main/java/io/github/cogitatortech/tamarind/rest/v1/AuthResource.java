package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import io.github.cogitatortech.tamarind.security.User;
import io.github.cogitatortech.tamarind.security.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** REST endpoint for authentication and user management. */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

  private static final Logger LOGGER = LogManager.getLogger(AuthResource.class);

  @Inject UserService userService;

  @POST
  @Path("/register")
  public Response register(RegisterRequest request) {
    try {
      if (request.username == null || request.username.trim().isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Username is required"))
            .build();
      }

      if (request.password == null || request.password.length() < 4) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Password must be at least 4 characters"))
            .build();
      }

      User user = userService.createUser(request.username, request.email, request.password);

      Map<String, Object> data = new HashMap<>();
      data.put("username", user.getUsername());
      data.put("email", user.getEmail());

      LOGGER.info("User registered: {}", request.username);
      return Response.ok(ApiResponse.success(data)).build();

    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(ApiResponse.error("USER_EXISTS", e.getMessage()))
          .build();
    } catch (Exception e) {
      LOGGER.error("Registration error", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Registration failed"))
          .build();
    }
  }

  @POST
  @Path("/login")
  public Response login(LoginRequest request) {
    try {
      if (request.username == null || request.password == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Username and password are required"))
            .build();
      }

      User user = userService.authenticate(request.username, request.password);

      if (user == null) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("INVALID_CREDENTIALS", "Invalid username or password"))
            .build();
      }

      String token = userService.generateToken(user);

      Map<String, Object> data = new HashMap<>();
      data.put("token", token);
      data.put(
          "user",
          Map.of(
              "username",
              user.getUsername(),
              "email",
              user.getEmail() != null ? user.getEmail() : "",
              "role",
              user.getRole().getName()));

      LOGGER.info("User logged in: {}", request.username);
      return Response.ok(ApiResponse.success(data)).build();

    } catch (Exception e) {
      LOGGER.error("Login error", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Login failed"))
          .build();
    }
  }

  @GET
  @Path("/users")
  public Response listUsers(@HeaderParam("Authorization") String authHeader) {
    try {
      // Simple token validation
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }

      String token = authHeader.substring(7);
      if (!userService.validateToken(token)) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid token"))
            .build();
      }

      List<User> users = userService.getAllUsers();
      List<Map<String, Object>> userData =
          users.stream()
              .map(
                  u -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", u.getUsername());
                    userMap.put("email", u.getEmail() != null ? u.getEmail() : "");
                    userMap.put("role", u.getRole().getName());
                    userMap.put("enabled", u.getEnabled());
                    userMap.put(
                        "lastLogin", u.getLastLogin() != null ? u.getLastLogin().toString() : null);
                    return userMap;
                  })
              .toList();

      return Response.ok(ApiResponse.success(userData)).build();

    } catch (Exception e) {
      LOGGER.error("Error listing users", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to list users"))
          .build();
    }
  }

  @PUT
  @Path("/users/{username}/role")
  public Response updateUserRole(
      @PathParam("username") String username,
      @HeaderParam("Authorization") String authHeader,
      RoleUpdateRequest request) {
    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }

      String token = authHeader.substring(7);
      String adminUsername = userService.getUsernameFromToken(token);

      if (adminUsername == null || !userService.isAdmin(adminUsername)) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(ApiResponse.error("FORBIDDEN", "Admin access required"))
            .build();
      }

      boolean updated = userService.updateUserRole(username, request.role);
      if (!updated) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(ApiResponse.error("NOT_FOUND", "User not found"))
            .build();
      }

      return Response.ok(
              ApiResponse.success(Map.of("username", username, "role", request.role.getName())))
          .build();

    } catch (Exception ex) {
      LOGGER.error("Error updating user role", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to update role"))
          .build();
    }
  }

  @DELETE
  @Path("/users/{username}")
  public Response deleteUser(
      @PathParam("username") String username, @HeaderParam("Authorization") String authHeader) {
    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ApiResponse.error("UNAUTHORIZED", "Invalid or missing token"))
            .build();
      }

      String token = authHeader.substring(7);
      String adminUsername = userService.getUsernameFromToken(token);

      if (adminUsername == null || !userService.isAdmin(adminUsername)) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(ApiResponse.error("FORBIDDEN", "Admin access required"))
            .build();
      }

      if (adminUsername.equals(username)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Cannot delete your own account"))
            .build();
      }

      boolean deleted = userService.deleteUser(username);
      if (!deleted) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(ApiResponse.error("NOT_FOUND", "User not found"))
            .build();
      }

      return Response.ok(ApiResponse.success(Map.of("deleted", username))).build();

    } catch (Exception ex) {
      LOGGER.error("Error deleting user", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to delete user"))
          .build();
    }
  }

  public static class RegisterRequest {
    public String username;
    public String email;
    public String password;
  }

  public static class LoginRequest {
    public String username;
    public String password;
  }

  public static class RoleUpdateRequest {
    public io.github.cogitatortech.tamarind.security.Role role;
  }
}
