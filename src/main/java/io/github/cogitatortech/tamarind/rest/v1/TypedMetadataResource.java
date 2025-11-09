package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Stable typed columns endpoint for UI and integrations. */
@Path("/api/v1/metadata")
@Produces(MediaType.APPLICATION_JSON)
public class TypedMetadataResource {
  private static final Logger LOGGER = LogManager.getLogger(TypedMetadataResource.class);

  @Inject QueryEngine engine;
  @Inject io.github.cogitatortech.tamarind.security.UserService userService;

  @GET
  @Path("/tables/{table}/columns")
  public Response getColumns(
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
      var cols = engine.getMetadata().getColumnsWithTypes(table);
      var sb = new StringBuilder();
      sb.append('{').append("\"data\":");
      sb.append('[');
      for (int i = 0; i < cols.size(); i++) {
        var c = cols.get(i);
        sb.append('{');
        sb.append("\"name\":\"").append(escapeJson(c.getName())).append('\"');
        if (c.getType() != null) {
          sb.append(',').append("\"type\":\"").append(escapeJson(c.getType())).append('\"');
        }
        sb.append('}');
        if (i < cols.size() - 1) sb.append(',');
      }
      sb.append(']').append('}');
      return Response.ok(sb.toString()).build();
    } catch (Exception e) {
      LOGGER.error("Error getting typed columns for {}", table, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to get columns"))
          .build();
    }
  }

  @GET
  @Path("/tables/{table}/profile")
  public Response profileTable(
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
      // Simple JSON streaming using StreamingOutput
      jakarta.ws.rs.core.StreamingOutput out =
          os -> {
            try (var conn =
                    ((io.github.cogitatortech.tamarind.engine.JdbcQueryEngine) engine)
                        .getJdbcConnection();
                var stmt = conn.createStatement()) {
              // get columns
              var cols = engine.getMetadata().getColumnsWithTypes(table);
              os.write('{');
              os.write("\"columns\":".getBytes());
              os.write('[');
              for (int i = 0; i < cols.size(); i++) {
                var c = cols.get(i);
                String colJson =
                    String.format(
                        "{\"name\":\"%s\",\"type\":\"%s\"}",
                        escapeJson(c.getName()), escapeJson(String.valueOf(c.getType())));
                os.write(colJson.getBytes());
                if (i < cols.size() - 1) os.write(',');
              }
              os.write(']');
              // counts
              var rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM " + table);
              long total = 0;
              if (rs.next()) total = rs.getLong(1);
              rs.close();
              String counts = String.format(",\"rowCount\":%d", total);
              os.write(counts.getBytes());
              // sample
              rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 100");
              var md = rs.getMetaData();
              int cc = md.getColumnCount();
              os.write(",\"sample\":[".getBytes());
              boolean first = true;
              while (rs.next()) {
                if (!first) os.write(',');
                first = false;
                os.write('{');
                for (int i = 1; i <= cc; i++) {
                  String name = md.getColumnLabel(i);
                  Object val = rs.getObject(i);
                  os.write(('\"' + escapeJson(name) + '\"' + ':').getBytes());
                  if (val == null) os.write("null".getBytes());
                  else if (val instanceof Number || val instanceof Boolean)
                    os.write(String.valueOf(val).getBytes());
                  else {
                    os.write(('\"' + escapeJson(String.valueOf(val)) + '\"').getBytes());
                  }
                  if (i < cc) os.write(',');
                }
                os.write('}');
              }
              os.write(']');
              os.write('}');
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          };
      return Response.ok(out).build();
    } catch (Exception e) {
      LOGGER.error("Error profiling table {}", table, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to profile table"))
          .build();
    }
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
