package io.github.cogitatortech.tamarind.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Tests for SystemResourceV1. */
@QuarkusTest
class SystemResourceV1Test {

  @Test
  void testSystemInfo() {
    given()
        .when()
        .get("/api/v1/system/info")
        .then()
        .statusCode(200)
        .body("name", equalTo("tamarind"))
        .body("status", equalTo("running"))
        .body("storage.oltp_backend", notNullValue())
        .body("storage.ephemeral_backend", notNullValue())
        .body("users.total_users", greaterThanOrEqualTo(0))
        .body("jvm.uptime_ms", greaterThan(0))
        .body("jvm.memory.heap_used_mb", greaterThan(0));
  }

  @Test
  void testSystemHealth() {
    given()
        .when()
        .get("/api/v1/system/health")
        .then()
        .statusCode(200)
        .body("status", anyOf(equalTo("UP"), equalTo("DEGRADED")))
        .body("timestamp", notNullValue())
        .body("checks", notNullValue())
        .body("checks.user_repository", notNullValue())
        .body("checks.ephemeral_repository", notNullValue());
  }

  @Test
  void testSystemInfoContainsStorageBackends() {
    given()
        .when()
        .get("/api/v1/system/info")
        .then()
        .statusCode(200)
        .body("storage.oltp_backend", anyOf(equalTo("in-memory"), equalTo("database (JPA)")))
        .body("storage.ephemeral_backend", equalTo("in-memory"));
  }

  @Test
  void testSystemInfoContainsVersion() {
    given()
        .when()
        .get("/api/v1/system/info")
        .then()
        .statusCode(200)
        .body("version", notNullValue())
        .body("version", not(emptyString()));
  }
}
