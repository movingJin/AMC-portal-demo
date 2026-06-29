package com.backend.amc_portal.common.config;

import com.backend.amc_portal.common.security.JsonAuthenticationEntryPoint;
import com.backend.amc_portal.common.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@Profile("keycloak")
@RequiredArgsConstructor
public class KeycloakSecurityConfig {

  private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;
  private final JsonAuthenticationEntryPoint authenticationEntryPoint;
  private final CorsConfigurationSource corsConfigurationSource;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/board/*/files/history",
                        "/api/board/*/files/downloads")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/board/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/board-master", "/api/board-master/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)));
    return http.build();
  }
}
