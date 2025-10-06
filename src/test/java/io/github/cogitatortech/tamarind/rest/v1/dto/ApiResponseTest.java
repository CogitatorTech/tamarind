package io.github.cogitatortech.tamarind.rest.v1.dto;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.engine.QueryResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for ApiResponse. */
class ApiResponseTest {

  @Test
  void testSuccessResponse() {
    QueryResult data = new QueryResult(List.of("id"), List.of(Map.of("id", 1)));
    ApiResponse<QueryResult> response = ApiResponse.success(data);

    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(data, response.getData());
    assertNull(response.getError());
  }

  @Test
  void testErrorResponse() {
    String errorCode = "INTERNAL_ERROR";
    String errorMessage = "Something went wrong";
    ApiResponse<Object> response = ApiResponse.error(errorCode, errorMessage);

    assertNotNull(response);
    assertFalse(response.isSuccess());
    assertNull(response.getData());
    assertNotNull(response.getError());
    assertEquals(errorCode, response.getError().getCode());
    assertEquals(errorMessage, response.getError().getMessage());
  }

  @Test
  void testSuccessResponseWithMetadata() {
    QueryResult data = new QueryResult(List.of("id"), List.of(Map.of("id", 1)));
    ApiResponse.ResponseMetadata metadata = new ApiResponse.ResponseMetadata(100L, 1, false, false);
    ApiResponse<QueryResult> response = ApiResponse.success(data, metadata);

    assertTrue(response.isSuccess());
    assertEquals(data, response.getData());
    assertNotNull(response.getMetadata());
    assertEquals(100L, response.getMetadata().getExecutionTimeMs());
    assertEquals(1, response.getMetadata().getRowCount());
  }

  @Test
  void testSettersAndGetters() {
    ApiResponse<String> response = new ApiResponse<>();

    response.setSuccess(true);
    response.setData("test data");

    ApiResponse.ApiError error = new ApiResponse.ApiError("ERR_CODE", "Error message", "Details");
    response.setError(error);

    ApiResponse.ResponseMetadata metadata = new ApiResponse.ResponseMetadata(50L, 10, true, false);
    response.setMetadata(metadata);

    assertTrue(response.isSuccess());
    assertEquals("test data", response.getData());
    assertNotNull(response.getError());
    assertEquals("ERR_CODE", response.getError().getCode());
    assertNotNull(response.getMetadata());
    assertEquals(50L, response.getMetadata().getExecutionTimeMs());
  }

  @Test
  void testApiErrorCreation() {
    ApiResponse.ApiError error =
        new ApiResponse.ApiError("NOT_FOUND", "Resource not found", "Table does not exist");

    assertEquals("NOT_FOUND", error.getCode());
    assertEquals("Resource not found", error.getMessage());
    assertEquals("Table does not exist", error.getDetails());
  }

  @Test
  void testApiErrorSetters() {
    ApiResponse.ApiError error = new ApiResponse.ApiError();

    error.setCode("INVALID_REQUEST");
    error.setMessage("Bad request");
    error.setDetails("SQL syntax error");

    assertEquals("INVALID_REQUEST", error.getCode());
    assertEquals("Bad request", error.getMessage());
    assertEquals("SQL syntax error", error.getDetails());
  }

  @Test
  void testResponseMetadataCreation() {
    ApiResponse.ResponseMetadata metadata = new ApiResponse.ResponseMetadata(200L, 50, true, false);

    assertEquals(200L, metadata.getExecutionTimeMs());
    assertEquals(50, metadata.getRowCount());
    assertTrue(metadata.getCached());
    assertFalse(metadata.getTruncated());
  }

  @Test
  void testResponseMetadataSetters() {
    ApiResponse.ResponseMetadata metadata = new ApiResponse.ResponseMetadata();

    metadata.setExecutionTimeMs(150L);
    metadata.setRowCount(25);
    metadata.setCached(false);
    metadata.setTruncated(true);

    assertEquals(150L, metadata.getExecutionTimeMs());
    assertEquals(25, metadata.getRowCount());
    assertFalse(metadata.getCached());
    assertTrue(metadata.getTruncated());
  }

  @Test
  void testErrorWithDetails() {
    ApiResponse<Object> response =
        ApiResponse.error("VALIDATION_ERROR", "Invalid input", "Email format is incorrect");

    assertFalse(response.isSuccess());
    assertNotNull(response.getError());
    assertEquals("VALIDATION_ERROR", response.getError().getCode());
    assertEquals("Invalid input", response.getError().getMessage());
    assertEquals("Email format is incorrect", response.getError().getDetails());
  }
}
