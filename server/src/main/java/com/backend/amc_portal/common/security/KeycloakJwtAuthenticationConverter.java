package com.backend.amc_portal.common.security;

import com.backend.amc_portal.auth.entity.User;
import com.backend.amc_portal.auth.entity.UserRole;
import com.backend.amc_portal.auth.entity.UserStatus;
import com.backend.amc_portal.auth.service.UserProvisioningService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Keycloak이 발급한 JWT를 검증한 뒤, 로컬 {@code portal.users}에 사용자를 JIT(Just-In-Time) provisioning하고 기존
 * {@link UserPrincipal} 기반 컨트롤러 코드가 그대로 동작하도록 인증 토큰을 만든다.
 */
@Component
@Profile("keycloak")
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserProvisioningService provisioningService;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String keycloakId = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String displayName = resolveDisplayName(jwt, email);
    UserRole role = extractRole(jwt);

    User user = provisioningService.provision(keycloakId, email, displayName, role);
    if (user.getStatus() == UserStatus.DISABLED) {
      throw new BadCredentialsException("비활성화된 계정입니다.");
    }

    UserPrincipal principal =
        new UserPrincipal(user.getId(), user.getEmail(), user.getRole().name());
    return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
  }

  private String resolveDisplayName(Jwt jwt, String email) {
    String name = jwt.getClaimAsString("name");
    if (name != null && !name.isBlank()) return name;
    String preferredUsername = jwt.getClaimAsString("preferred_username");
    if (preferredUsername != null && !preferredUsername.isBlank()) return preferredUsername;
    return email;
  }

  @SuppressWarnings("unchecked")
  private UserRole extractRole(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess == null) return UserRole.USER;
    Object rolesClaim = realmAccess.get("roles");
    if (rolesClaim instanceof List<?> roles && roles.contains("ADMIN")) {
      return UserRole.ADMIN;
    }
    return UserRole.USER;
  }
}
