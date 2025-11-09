package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import io.github.cogitatortech.tamarind.rest.v1.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Simple cursor based pagination for large result sets (in-memory). */
@Path("/api/v1/query/cursor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CursorQueryResource {
  private static final Logger LOGGER = LogManager.getLogger(CursorQueryResource.class);
  private static final int DEFAULT_PAGE_SIZE = 500;
  private static final long CURSOR_TTL_MS = 5 * 60 * 1000; // 5 minutes

  private static class CursorState {
    final List<Map<String, Object>> rows;
    final List<String> columns;
    int position;
    final long expiresAt;

    CursorState(QueryResult qr) {
      this.rows = qr.getRows();
      this.columns = qr.getColumnNames();
      this.position = 0;
      this.expiresAt = System.currentTimeMillis() + CURSOR_TTL_MS;
    }
  }

  private static final Map<String, CursorState> CURSORS = new ConcurrentHashMap<>();

  @Inject QueryEngine engine;
  @Inject io.github.cogitatortech.tamarind.security.UserService userService;

  public static class CursorRequest {
    public String sql;
    public String cursorId;
    public Integer pageSize;
  }

  @POST
  public Response openOrNext(@HeaderParam("Authorization") String authHeader, CursorRequest req) {
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
      int pageSize = (req.pageSize != null && req.pageSize > 0) ? req.pageSize : DEFAULT_PAGE_SIZE;

      if (req.cursorId == null) {
        if (req.sql == null || req.sql.isBlank()) {
          return Response.status(Response.Status.BAD_REQUEST)
              .entity(ApiResponse.error("INVALID_REQUEST", "SQL is required for new cursor"))
              .build();
        }
        QueryResult qr = engine.executeQuery(req.sql);
        String cursorId = UUID.randomUUID().toString();
        CURSORS.put(cursorId, new CursorState(qr));
        return Response.ok(ApiResponse.success(pageChunk(cursorId, pageSize))).build();
      } else {
        CursorState state = CURSORS.get(req.cursorId);
        if (state == null || state.expiresAt < System.currentTimeMillis()) {
          CURSORS.remove(req.cursorId);
          return Response.status(Response.Status.NOT_FOUND)
              .entity(ApiResponse.error("CURSOR_EXPIRED", "Cursor not found or expired"))
              .build();
        }
        return Response.ok(ApiResponse.success(pageChunk(req.cursorId, pageSize))).build();
      }
    } catch (SQLException e) {
      LOGGER.error("SQL error in cursor query", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("SQL_ERROR", "Query failed", e.getMessage()))
          .build();
    } catch (Exception e) {
      LOGGER.error("Unexpected error in cursor query", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ApiResponse.error("INTERNAL_ERROR", "Unexpected error", e.getMessage()))
          .build();
    }
  }

  private Map<String, Object> pageChunk(String cursorId, int pageSize) {
    CursorState st = CURSORS.get(cursorId);
    int start = st.position;
    int end = Math.min(st.rows.size(), start + pageSize);
    List<Map<String, Object>> slice = st.rows.subList(start, end);
    st.position = end;
    boolean hasMore = st.position < st.rows.size();
    if (!hasMore) CURSORS.remove(cursorId); // auto cleanup
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("cursorId", cursorId);
    m.put("columns", st.columns);
    m.put("rows", slice);
    m.put("hasMore", hasMore);
    m.put("totalRows", st.rows.size());
    m.put("returnedRows", slice.size());
    return m;
  }
}
