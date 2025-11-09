package io.github.cogitatortech.tamarind.rest;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.github.cogitatortech.tamarind.rest.v1.TypedMetadataResource;
import io.github.cogitatortech.tamarind.security.UserService;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TypedColumnsResourceTest {
  private QueryEngine engine;
  private UserService userService;
  private TypedMetadataResource resource;

  @BeforeEach
  void setup() throws Exception {
    engine = Mockito.mock(QueryEngine.class);
    userService = Mockito.mock(UserService.class);
    resource = new TypedMetadataResource();
    // Inject mocks via reflection (simplest without CDI boot)
    var engineField = TypedMetadataResource.class.getDeclaredField("engine");
    engineField.setAccessible(true);
    engineField.set(resource, engine);
    var userField = TypedMetadataResource.class.getDeclaredField("userService");
    userField.setAccessible(true);
    userField.set(resource, userService);
  }

  @Test
  void rejectsUnauthorized() {
    Response r = resource.getColumns(null, "my_table");
    assertEquals(401, r.getStatus());
  }

  @Test
  void returnsTypedColumns() throws Exception {
    Mockito.when(userService.validateToken("tok")).thenReturn(true);
    var meta = Mockito.mock(QueryEngine.DatabaseMetadata.class);
    Mockito.when(engine.getMetadata()).thenReturn(meta);
    var c1 = new QueryEngine.ColumnMeta("id", "INTEGER");
    var c2 = new QueryEngine.ColumnMeta("name", "VARCHAR");
    Mockito.when(meta.getColumnsWithTypes("my_table")).thenReturn(List.of(c1, c2));
    Response r = resource.getColumns("Bearer tok", "my_table");
    assertEquals(200, r.getStatus());
    String json = r.getEntity().toString();
    assertTrue(json.contains("id"));
    assertTrue(json.contains("INTEGER"));
  }
}
