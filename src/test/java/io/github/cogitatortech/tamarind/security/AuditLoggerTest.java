package io.github.cogitatortech.tamarind.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditLoggerTest {

  private AuditLogger auditLogger;

  @BeforeEach
  void setUp() {
    auditLogger = new AuditLogger();
  }

  @Test
  void testLogQuery() {
    // Should not throw any exceptions
    assertDoesNotThrow(() -> auditLogger.logQuery("testUser", "SELECT * FROM test", 100L, 50));
  }

  @Test
  void testLogAuthentication() {
    assertDoesNotThrow(() -> auditLogger.logAuthentication("testUser", true, "JWT"));
  }

  @Test
  void testLogRateLimitViolation() {
    assertDoesNotThrow(() -> auditLogger.logRateLimitViolation("testUser", "/query"));
  }

  @Test
  void testLogSecurityViolation() {
    assertDoesNotThrow(
        () -> auditLogger.logSecurityViolation("testUser", "SQL_INJECTION", "DROP TABLE"));
  }

  @Test
  void testLogError() {
    assertDoesNotThrow(
        () -> auditLogger.logError("testUser", "SELECT * FROM test", "Table not found"));
  }

  @Test
  void testLogQueryWithLongQuery() {
    // Test with a very long query
    String longQuery = "SELECT * FROM test WHERE " + "a=1 AND ".repeat(100);
    assertDoesNotThrow(() -> auditLogger.logQuery("testUser", longQuery, 100L, 50));
  }

  @Test
  void testLogQueryWithNullQuery() {
    // Should handle null gracefully
    assertDoesNotThrow(() -> auditLogger.logQuery("testUser", null, 100L, 50));
  }
}
