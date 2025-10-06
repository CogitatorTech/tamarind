package io.github.cogitatortech.tamarind.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Identity provider that validates tokens using UserService. */
@ApplicationScoped
public class TokenIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

  private static final Logger LOGGER = LogManager.getLogger(TokenIdentityProvider.class);

  @Inject UserService userService;

  @Override
  public Class<TokenAuthenticationRequest> getRequestType() {
    return TokenAuthenticationRequest.class;
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      TokenAuthenticationRequest request, AuthenticationRequestContext context) {

    return Uni.createFrom()
        .item(
            () -> {
              if (!(request.getToken() instanceof TokenCredential)) {
                LOGGER.warn("Invalid token type received");
                throw new AuthenticationFailedException("Invalid token type");
              }

              TokenCredential tokenCredential = (TokenCredential) request.getToken();
              String token = tokenCredential.getToken();

              LOGGER.info(
                  "Validating token: {}...", token.substring(0, Math.min(8, token.length())));

              if (!userService.validateToken(token)) {
                LOGGER.warn("Token validation failed");
                throw new AuthenticationFailedException("Invalid token");
              }

              String username = userService.getUsernameFromToken(token);
              LOGGER.info("Token validated successfully for user: {}", username);

              return QuarkusSecurityIdentity.builder()
                  .setPrincipal(new QuarkusPrincipal(username))
                  .addRole("user")
                  .build();
            });
  }
}
