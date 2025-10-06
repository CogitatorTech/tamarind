package io.github.cogitatortech.tamarind.config;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

/**
 * Tests for security headers filter
 *
 * <p>Verifies that all required security headers are present in HTTP responses.
 */
@QuarkusTest
public class SecurityHeadersFilterTest {

  @Test
  public void testContentSecurityPolicyHeader() {
    String cspHeader = RestAssured.get("/").then().extract().header("Content-Security-Policy");

    assertNotNull(cspHeader, "Content-Security-Policy header should be present");
    assertTrue(cspHeader.contains("default-src 'self'"), "CSP should restrict default-src to self");
    assertTrue(cspHeader.contains("script-src"), "CSP should include script-src directive");
    assertTrue(cspHeader.contains("frame-ancestors 'none'"), "CSP should prevent framing");
  }

  @Test
  public void testXFrameOptionsHeader() {
    String xFrameOptions = RestAssured.get("/").then().extract().header("X-Frame-Options");

    assertNotNull(xFrameOptions, "X-Frame-Options header should be present");
    assertEquals("DENY", xFrameOptions, "X-Frame-Options should be set to DENY");
  }

  @Test
  public void testXContentTypeOptionsHeader() {
    String xContentTypeOptions =
        RestAssured.get("/").then().extract().header("X-Content-Type-Options");

    assertNotNull(xContentTypeOptions, "X-Content-Type-Options header should be present");
    assertEquals("nosniff", xContentTypeOptions, "X-Content-Type-Options should be set to nosniff");
  }

  @Test
  public void testXXssProtectionHeader() {
    String xXssProtection = RestAssured.get("/").then().extract().header("X-XSS-Protection");

    assertNotNull(xXssProtection, "X-XSS-Protection header should be present");
    assertEquals(
        "1; mode=block", xXssProtection, "X-XSS-Protection should be enabled with blocking mode");
  }

  @Test
  public void testReferrerPolicyHeader() {
    String referrerPolicy = RestAssured.get("/").then().extract().header("Referrer-Policy");

    assertNotNull(referrerPolicy, "Referrer-Policy header should be present");
    assertEquals(
        "strict-origin-when-cross-origin",
        referrerPolicy,
        "Referrer-Policy should be set to strict-origin-when-cross-origin");
  }

  @Test
  public void testPermissionsPolicyHeader() {
    String permissionsPolicy = RestAssured.get("/").then().extract().header("Permissions-Policy");

    assertNotNull(permissionsPolicy, "Permissions-Policy header should be present");
    assertTrue(
        permissionsPolicy.contains("geolocation=()"),
        "Permissions-Policy should disable geolocation");
    assertTrue(
        permissionsPolicy.contains("microphone=()"),
        "Permissions-Policy should disable microphone");
    assertTrue(permissionsPolicy.contains("camera=()"), "Permissions-Policy should disable camera");
  }

  @Test
  public void testSecurityHeadersOnApiEndpoints() {
    String cspHeader =
        RestAssured.get("/api/v1/system/info").then().extract().header("Content-Security-Policy");

    assertNotNull(cspHeader, "Security headers should be present on API endpoints");
  }

  @Test
  public void testSecurityHeadersOnStaticResources() {
    String xFrameOptions =
        RestAssured.get("/index.html").then().extract().header("X-Frame-Options");

    assertNotNull(xFrameOptions, "Security headers should be present on static resources");
  }
}
