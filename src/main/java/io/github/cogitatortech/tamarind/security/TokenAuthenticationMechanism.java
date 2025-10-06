package io.github.cogitatortech.tamarind.security;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.Collections;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Custom HTTP authentication mechanism for Bearer token authentication. */
@Alternative
@Priority(1)
@ApplicationScoped
public class TokenAuthenticationMechanism implements HttpAuthenticationMechanism {

  private static final Logger LOGGER = LogManager.getLogger(TokenAuthenticationMechanism.class);
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  public Uni<SecurityIdentity> authenticate(
      RoutingContext context, IdentityProviderManager identityProviderManager) {

    String path = context.request().path();

    // Allow unauthenticated access to auth endpoints
    if (path.startsWith("/api/v1/auth/") || path.equals("/index.html") || path.equals("/")) {
      LOGGER.debug("Skipping authentication for public path: {}", path);
      return Uni.createFrom().nullItem();
    }

    String authHeader = context.request().getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      LOGGER.debug("No valid Authorization header found for path: {}", path);
      return Uni.createFrom().nullItem();
    }

    String token = authHeader.substring(BEARER_PREFIX.length());
    LOGGER.debug(
        "Extracted token for authentication: {}...",
        token.substring(0, Math.min(8, token.length())));

    // Use Quarkus's TokenCredential instead of custom BearerToken
    TokenAuthenticationRequest request =
        new TokenAuthenticationRequest(new TokenCredential(token, "bearer"));
    return identityProviderManager.authenticate(request);
  }

  @Override
  public Uni<ChallengeData> getChallenge(RoutingContext context) {
    LOGGER.debug("Returning 401 challenge for path: {}", context.request().path());
    return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
    return Collections.singleton(TokenAuthenticationRequest.class);
  }

  @Override
  public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
    return Uni.createFrom()
        .item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer"));
  }
}
