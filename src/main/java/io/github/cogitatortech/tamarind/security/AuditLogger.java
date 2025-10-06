package io.github.cogitatortech.tamarind.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Audit logging service for tracking query execution and security events with structured JSON
 * logging.
 */
@ApplicationScoped
public class AuditLogger {

  private static final Logger LOGGER = LogManager.getLogger(AuditLogger.class);
  private static final Logger AUDIT_LOGGER = LogManager.getLogger("AUDIT");
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  /** Log a query execution event. */
  public void logQuery(String userId, String query, long executionTimeMs, int rowCount) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "QUERY_EXECUTION");
    auditEvent.put("category", "DATA_ACCESS");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("query", truncate(query, 200));
    auditEvent.put("executionTimeMs", executionTimeMs);
    auditEvent.put("rowCount", rowCount);
    auditEvent.put("severity", "INFO");

    logStructured(auditEvent, "info");
  }

  /** Log a transaction event. */
  public void logTransaction(String userId, String operation, boolean success, String details) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "TRANSACTION");
    auditEvent.put("category", "DATA_MODIFICATION");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("operation", operation);
    auditEvent.put("success", success);
    auditEvent.put("details", details);
    auditEvent.put("severity", success ? "INFO" : "ERROR");

    logStructured(auditEvent, success ? "info" : "error");
  }

  /** Log an authentication event. */
  public void logAuthentication(String userId, boolean success, String method) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "AUTHENTICATION");
    auditEvent.put("category", "SECURITY");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("success", success);
    auditEvent.put("method", method);
    auditEvent.put("severity", success ? "INFO" : "WARN");

    logStructured(auditEvent, success ? "info" : "warn");
  }

  /** Log a rate limit violation. */
  public void logRateLimitViolation(String userId, String endpoint) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "RATE_LIMIT_VIOLATION");
    auditEvent.put("category", "SECURITY");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("endpoint", endpoint);
    auditEvent.put("severity", "WARN");

    logStructured(auditEvent, "warn");
  }

  /** Log a security violation. */
  public void logSecurityViolation(String userId, String violation, String details) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "SECURITY_VIOLATION");
    auditEvent.put("category", "SECURITY");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("violation", violation);
    auditEvent.put("details", details);
    auditEvent.put("severity", "ERROR");

    logStructured(auditEvent, "error");
  }

  /** Log an error event. */
  public void logError(String userId, String query, String errorMessage) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "QUERY_ERROR");
    auditEvent.put("category", "ERROR");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("query", truncate(query, 200));
    auditEvent.put("error", errorMessage);
    auditEvent.put("severity", "ERROR");

    logStructured(auditEvent, "error");
  }

  /** Log a data modification event. */
  public void logDataModification(
      String userId, String operation, String target, int affectedRows) {
    Map<String, Object> auditEvent = new HashMap<>();
    auditEvent.put("event", "DATA_MODIFICATION");
    auditEvent.put("category", "DATA_ACCESS");
    auditEvent.put("timestamp", Instant.now().toString());
    auditEvent.put("userId", userId);
    auditEvent.put("operation", operation);
    auditEvent.put("target", target);
    auditEvent.put("affectedRows", affectedRows);
    auditEvent.put("severity", "INFO");

    logStructured(auditEvent, "info");
  }

  /** Log structured audit event as JSON. */
  private void logStructured(Map<String, Object> auditEvent, String level) {
    try {
      String jsonLog = JSON_MAPPER.writeValueAsString(auditEvent);

      switch (level) {
        case "error":
          AUDIT_LOGGER.error(jsonLog);
          break;
        case "warn":
          AUDIT_LOGGER.warn(jsonLog);
          break;
        case "info":
        default:
          AUDIT_LOGGER.info(jsonLog);
          break;
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize audit event to JSON", e);
      AUDIT_LOGGER.info("Audit: {}", auditEvent);
    }
  }

  private String truncate(String str, int maxLength) {
    if (str == null) {
      return null;
    }
    return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
  }
}
