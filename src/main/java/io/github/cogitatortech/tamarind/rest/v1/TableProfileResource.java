package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Table profiling endpoint: returns basic statistics for columns of a table. */
@Path("/api/v1/metadata/profile")
@Produces(MediaType.APPLICATION_JSON)
public class TableProfileResource {
  private static final Logger LOGGER = LogManager.getLogger(TableProfileResource.class);

  @Inject QueryEngine engine;
  @Inject io.github.cogitatortech.tamarind.security.UserService userService;

  @GET
  @Path("/{table}")
  public Response profile(
      @HeaderParam("Authorization") String authHeader, @PathParam("table") String table) {
    try {
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
      if (table == null || table.isBlank()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(ApiResponse.error("INVALID_REQUEST", "Table name is required"))
            .build();
      }

      // Collect basic column metadata first
      List<QueryEngine.ColumnMeta> columns;
      try {
        columns = engine.getMetadata().getColumnsWithTypes(table);
      } catch (SQLException e) {
        LOGGER.error("Failed to get column metadata for {}", table, e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to get column metadata"))
            .build();
      }
      if (columns.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(ApiResponse.error("NOT_FOUND", "Table not found: " + table))
            .build();
      }

      // Row count
      long rowCount = singleLong("SELECT COUNT(*) AS c FROM " + table);

      List<Map<String, Object>> colStats = new ArrayList<>();
      for (QueryEngine.ColumnMeta meta : columns) {
        String col = meta.getName();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("name", col);
        stats.put("type", meta.getType());
        stats.put("rowCount", rowCount);
        try {
          long nonNull = singleLong("SELECT COUNT(" + col + ") FROM " + table);
          long distinct = singleLong("SELECT COUNT(DISTINCT " + col + ") FROM " + table);
          long nulls = rowCount - nonNull;
          stats.put("nullCount", nulls);
          stats.put("distinctCount", distinct);
          if (isNumericType(meta.getType())) {
            // Compute min/max/avg in one query to reduce round trips
            String aggSql =
                "SELECT MIN("
                    + col
                    + "), MAX("
                    + col
                    + "), AVG("
                    + col
                    + ") FROM "
                    + table
                    + " WHERE "
                    + col
                    + " IS NOT NULL";
            List<Map<String, Object>> rows = engine.executeQuery(aggSql).getRows();
            if (!rows.isEmpty()) {
              Map<String, Object> r = rows.get(0);
              stats.put("min", r.values().toArray()[0]);
              stats.put("max", r.values().toArray()[1]);
              stats.put("avg", r.values().toArray()[2]);
            }
          }
        } catch (Exception ex) {
          stats.put("error", ex.getMessage());
          LOGGER.debug("Profiling error for column {}: {}", col, ex.toString());
        }
        colStats.add(stats);
      }

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("table", table);
      payload.put("rowCount", rowCount);
      payload.put("columns", colStats);
      return Response.ok(ApiResponse.success(payload)).build();

    } catch (Exception e) {
      LOGGER.error("Error profiling table {}", table, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to profile table"))
          .build();
    }
  }

  private boolean isNumericType(String type) {
    if (type == null) return false;
    String t = type.toLowerCase(Locale.ROOT);
    return t.contains("int")
        || t.contains("decimal")
        || t.contains("double")
        || t.contains("float")
        || t.contains("real")
        || t.contains("numeric");
  }

  private long singleLong(String sql) throws SQLException {
    var result = engine.executeQuery(sql).getRows();
    if (result.isEmpty()) return 0L;
    Object v = result.get(0).values().toArray()[0];
    if (v instanceof Number n) return n.longValue();
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (NumberFormatException e) {
      return 0L;
    }
  }
}
