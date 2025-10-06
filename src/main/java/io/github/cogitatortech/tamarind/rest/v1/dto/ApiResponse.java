package io.github.cogitatortech.tamarind.rest.v1.dto;

import java.util.Map;

public class ApiResponse<T> {
  private boolean success;
  private T data;
  private ApiError error;
  private ResponseMetadata metadata;

  public ApiResponse() {}

  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.data = data;
    return response;
  }

  public static <T> ApiResponse<T> success(T data, ResponseMetadata metadata) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.data = data;
    response.metadata = metadata;
    return response;
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = false;
    response.error = new ApiError(code, message, null);
    return response;
  }

  public static <T> ApiResponse<T> error(String code, String message, String details) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = false;
    response.error = new ApiError(code, message, details);
    return response;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public ApiError getError() {
    return error;
  }

  public void setError(ApiError error) {
    this.error = error;
  }

  public ResponseMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ResponseMetadata metadata) {
    this.metadata = metadata;
  }

  public static class ApiError {
    private String code;
    private String message;
    private String details;

    public ApiError() {}

    public ApiError(String code, String message, String details) {
      this.code = code;
      this.message = message;
      this.details = details;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getDetails() {
      return details;
    }

    public void setDetails(String details) {
      this.details = details;
    }
  }

  public static class ResponseMetadata {
    private Long executionTimeMs;
    private Integer rowCount;
    private Boolean cached;
    private Boolean truncated;
    private Map<String, Object> additional;

    public ResponseMetadata() {}

    public ResponseMetadata(
        Long executionTimeMs, Integer rowCount, Boolean cached, Boolean truncated) {
      this.executionTimeMs = executionTimeMs;
      this.rowCount = rowCount;
      this.cached = cached;
      this.truncated = truncated;
    }

    public Long getExecutionTimeMs() {
      return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
      this.executionTimeMs = executionTimeMs;
    }

    public Integer getRowCount() {
      return rowCount;
    }

    public void setRowCount(Integer rowCount) {
      this.rowCount = rowCount;
    }

    public Boolean getCached() {
      return cached;
    }

    public void setCached(Boolean cached) {
      this.cached = cached;
    }

    public Boolean getTruncated() {
      return truncated;
    }

    public void setTruncated(Boolean truncated) {
      this.truncated = truncated;
    }

    public Map<String, Object> getAdditional() {
      return additional;
    }

    public void setAdditional(Map<String, Object> additional) {
      this.additional = additional;
    }
  }
}
