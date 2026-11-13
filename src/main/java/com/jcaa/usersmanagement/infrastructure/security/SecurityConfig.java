package com.jcaa.usersmanagement.infrastructure.security;

import static com.jcaa.usersmanagement.infrastructure.security.OwnerOrRoleAuthorizationManager.ownerOrAnyRole;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final String AUTH_LOGIN_PATH = "/api/auth/login";
  private static final String USERS_PATH = "/api/users";
  private static final String USERS_DETAIL_PATH = "/api/users/**";
  private static final String PROFILE_PHOTO_PATH = "/api/users/{id}/profile-photo";
  private static final String SEARCH_REINDEX_PATH = "/api/users/search/reindex";
  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_REVIEWER = "REVIEWER";
  private static final String[] OPEN_API_PATHS = {
    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      final HttpSecurity http,
      final JwtAuthenticationFilter jwtAuthenticationFilter,
      final RestAuthenticationEntryPoint authenticationEntryPoint,
      final RestAccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(AUTH_LOGIN_PATH)
                    .permitAll()
                    .requestMatchers(OPEN_API_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, USERS_PATH)
                    .permitAll()
                    // Foto de perfil: el propio usuario o los roles indicados
                    .requestMatchers(HttpMethod.GET, PROFILE_PHOTO_PATH)
                    .access(ownerOrAnyRole(ROLE_ADMIN, ROLE_REVIEWER))
                    .requestMatchers(HttpMethod.POST, PROFILE_PHOTO_PATH)
                    .access(ownerOrAnyRole(ROLE_ADMIN))
                    .requestMatchers(HttpMethod.DELETE, PROFILE_PHOTO_PATH)
                    .access(ownerOrAnyRole(ROLE_ADMIN))
                    // Reconstruir el indice de OpenSearch
                    .requestMatchers(HttpMethod.POST, SEARCH_REINDEX_PATH)
                    .hasRole(ROLE_ADMIN)
                    // GET /api/users/search queda cubierto por esta regla (ADMIN, REVIEWER)
                    .requestMatchers(HttpMethod.GET, USERS_PATH, USERS_DETAIL_PATH)
                    .hasAnyRole(ROLE_ADMIN, ROLE_REVIEWER)
                    .requestMatchers(HttpMethod.PUT, USERS_DETAIL_PATH)
                    .hasRole(ROLE_ADMIN)
                    .requestMatchers(HttpMethod.DELETE, USERS_DETAIL_PATH)
                    .hasRole(ROLE_ADMIN)
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
