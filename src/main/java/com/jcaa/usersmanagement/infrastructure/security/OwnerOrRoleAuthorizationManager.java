package com.jcaa.usersmanagement.infrastructure.security;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Permite el acceso si el usuario autenticado es el dueño del recurso (su id coincide con la
 * variable {@code {id}} de la ruta) o si tiene alguno de los roles indicados.
 *
 * <p>Ejemplo: un MEMBER puede subir su propia foto de perfil, pero no la de otro usuario.
 */
public final class OwnerOrRoleAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  private static final String ROLE_PREFIX = "ROLE_";
  private static final String ID_PATH_VARIABLE = "id";

  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
  private final Set<String> allowedAuthorities;

  private OwnerOrRoleAuthorizationManager(final Set<String> allowedAuthorities) {
    this.allowedAuthorities = allowedAuthorities;
  }

  public static OwnerOrRoleAuthorizationManager ownerOrAnyRole(final String... roles) {
    return new OwnerOrRoleAuthorizationManager(
        Arrays.stream(roles)
            .map(role -> ROLE_PREFIX + role)
            .collect(Collectors.toUnmodifiableSet()));
  }

  @Override
  public AuthorizationDecision check(
      final Supplier<Authentication> authenticationSupplier,
      final RequestAuthorizationContext context) {
    final Authentication authentication = authenticationSupplier.get();
    if (!isAuthenticated(authentication)) {
      return new AuthorizationDecision(false);
    }
    return new AuthorizationDecision(
        hasAllowedRole(authentication) || isOwner(authentication, context));
  }

  private boolean isAuthenticated(final Authentication authentication) {
    return Objects.nonNull(authentication)
        && authentication.isAuthenticated()
        && !trustResolver.isAnonymous(authentication);
  }

  private boolean hasAllowedRole(final Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(allowedAuthorities::contains);
  }

  private static boolean isOwner(
      final Authentication authentication, final RequestAuthorizationContext context) {
    final String resourceOwnerId = context.getVariables().get(ID_PATH_VARIABLE);
    return Objects.nonNull(resourceOwnerId) && resourceOwnerId.equals(authentication.getName());
  }
}
