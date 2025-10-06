package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** V1 REST API for metadata discovery and database introspection. */
@Path("/api/v1/metadata")
@Produces(MediaType.APPLICATION_JSON)
public class MetadataResourceV1 {

  private static final Logger LOGGER = LogManager.getLogger(MetadataResourceV1.class);

  @Inject QueryEngine engine;

  @GET
  @Path("/tables")
  public Response listTables() {
    try {
      List<String> tables = engine.getMetadata().getTables();
      return Response.ok(ApiResponse.success(tables)).build();

    } catch (Exception e) {
      LOGGER.error("Error fetching table list", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Failed to fetch tables", e.getMessage()))
          .build();
    }
  }

  @GET
  @Path("/tables/{tableName}/schema")
  public Response getTableSchema(@PathParam("tableName") String tableName) {
    try {
      List<String> columns = engine.getMetadata().getColumns(tableName);

      if (columns.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(ApiResponse.error("NOT_FOUND", "Table not found: " + tableName))
            .build();
      }

      Map<String, Object> data = new HashMap<>();
      data.put("tableName", tableName);
      data.put("columns", columns);
      data.put("columnCount", columns.size());

      return Response.ok(ApiResponse.success(data)).build();
    } catch (Exception e) {
      LOGGER.error("Error fetching table schema for {}", tableName, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              ApiResponse.error("INTERNAL_ERROR", "Failed to fetch table schema", e.getMessage()))
          .build();
    }
  }

  @GET
  @Path("/database")
  public Response getDatabaseInfo() {
    try {
      QueryEngine.DatabaseMetadata metadata = engine.getMetadata();

      Map<String, Object> data = new HashMap<>();
      data.put("productName", metadata.getDatabaseProductName());
      data.put("productVersion", metadata.getDatabaseProductVersion());
      data.put("engineName", engine.getEngineName());
      data.put("engineVersion", engine.getEngineVersion());

      return Response.ok(ApiResponse.success(data)).build();
    } catch (Exception e) {
      LOGGER.error("Error fetching database info", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              ApiResponse.error("INTERNAL_ERROR", "Failed to fetch database info", e.getMessage()))
          .build();
    }
  }
}
