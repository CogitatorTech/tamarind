package io.github.cogitatortech.tamarind.config;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.http.HttpServerResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Security headers configuration for web UI protection
 *
 * <p>Adds essential security headers to HTTP responses: - Content-Security-Policy: Prevents XSS
 * attacks - X-Frame-Options: Prevents clickjacking - X-Content-Type-Options: Prevents MIME sniffing
 * - X-XSS-Protection: Legacy XSS protection - Strict-Transport-Security: Enforces HTTPS
 */
@ApplicationScoped
public class SecurityHeadersFilter {

  private static final Logger LOGGER = LogManager.getLogger(SecurityHeadersFilter.class);

  // Content Security Policy - allows CDN resources needed by CodeMirror and Vega
  // while still providing XSS protection
  private static final String CSP_HEADER =
      "default-src 'self'; "
          + "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; "
          + "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; "
          + "font-src 'self'; "
          + "img-src 'self' data:; "
          + "connect-src 'self'; "
          + "frame-ancestors 'none'";

  public void registerSecurityHeaders(@Observes Filters filters) {
    LOGGER.info("Registering security headers filter");

    filters.register(
        fc -> {
          HttpServerResponse response = fc.response();

          // Content Security Policy - prevents XSS attacks
          response.putHeader("Content-Security-Policy", CSP_HEADER);

          // X-Frame-Options - prevents clickjacking
          // Using DENY to prevent any framing
          response.putHeader("X-Frame-Options", "DENY");

          // X-Content-Type-Options - prevents MIME sniffing
          // Forces browser to respect Content-Type header
          response.putHeader("X-Content-Type-Options", "nosniff");

          // X-XSS-Protection - legacy XSS protection for older browsers
          // Modern browsers rely on CSP, but this adds defense in depth
          response.putHeader("X-XSS-Protection", "1; mode=block");

          // Strict-Transport-Security - enforces HTTPS
          // Only add if request is over HTTPS to avoid browser warnings
          if (fc.request().isSSL()) {
            response.putHeader(
                "Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
          }

          // Referrer-Policy - controls how much referrer information is shared
          response.putHeader("Referrer-Policy", "strict-origin-when-cross-origin");

          // Permissions-Policy - disables unnecessary browser features
          response.putHeader(
              "Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

          fc.next();
        },
        10); // Priority 10 to run early in filter chain
  }
}
