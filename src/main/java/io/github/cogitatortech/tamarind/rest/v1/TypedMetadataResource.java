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
      var out = new ArrayList<Map<String, Object>>(cols.size());
      for (var c : cols) {
        var m = new LinkedHashMap<String, Object>();
        m.put("name", c.getName());
        if (c.getType() != null) m.put("type", c.getType());
        out.add(m);
      }
      return Response.ok(ApiResponse.success(out)).build();
    } catch (Exception e) {
      LOGGER.error("Error getting typed columns for {}", table, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to get columns"))
          .build();
    }
  }
}
