package io.github.cogitatortech.tamarind.rest.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.github.cogitatortech.tamarind.service.QueryExecutionManager;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class QueryResourceV1CancelTest {

  private QueryResourceV1 resource;
  @Mock private QueryExecutionManager execManager;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resource = new QueryResourceV1();
    resource.execManager = execManager;
  }

  @Test
  void cancelReturnsAcceptedWhenFound() {
    when(execManager.cancelQuery("abc")).thenReturn(true);

    Response resp = resource.cancel("abc");
    assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
  }

  @Test
  void cancelReturnsNotFoundWhenMissing() {
    when(execManager.cancelQuery("missing")).thenReturn(false);

    Response resp = resource.cancel("missing");
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
  }

  @Test
  void cancelReturnsServiceUnavailableWhenManagerNull() {
    QueryResourceV1 res2 = new QueryResourceV1();
    Response resp = res2.cancel("id");
    assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
  }
}
