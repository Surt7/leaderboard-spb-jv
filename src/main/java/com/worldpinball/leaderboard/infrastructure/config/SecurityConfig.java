package com.worldpinball.leaderboard.infrastructure.config;

import com.worldpinball.leaderboard.infrastructure.security.authentication.ClaimsPrincipal;
import com.worldpinball.leaderboard.infrastructure.security.authentication.OperatorTokenAuthenticationHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final OperatorTokenAuthenticationHandler operatorTokenHandler;

    public SecurityConfig(OperatorTokenAuthenticationHandler operatorTokenHandler) {
        this.operatorTokenHandler = operatorTokenHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public OpenAPI and Swagger documentation
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Public health and observability probes
                        .requestMatchers(
                                "/alive",
                                "/health",
                                "/actuator/**"
                        ).permitAll()
                        // Public catalogue and leaderboard endpoints
                        .requestMatchers(
                                "/api/v1/gamemodes/**",
                                "/api/v1/game-modes/**",
                                "/api/v1/iplicenses/**",
                                "/api/v1/ip-licenses/**",
                                "/api/v1/leaderboards/**"
                        ).permitAll()
                        // Machine & Workstation public enrollment
                        .requestMatchers(
                                "/api/v1/machines/enroll",
                                "/api/v1/workstations/enroll"
                        ).permitAll()
                        // GraphQL endpoint (field-level auth enforced in resolver)
                        .requestMatchers(
                                "/graphql"
                        ).permitAll()
                        // Local developer test endpoints
                        .requestMatchers(
                                "/api/v1/test/**"
                        ).permitAll()
                        // Permission-protected admin endpoints evaluated by @RequirePermission AOP aspect
                        .requestMatchers(
                                "/api/v1/admin/certificates/**",
                                "/api/v1/admin/machines/provision",
                                "/api/v1/admin/machines/*/tokens"
                        ).permitAll()
                        // Admin-only endpoints protected by Keycloak Admin role
                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).hasAnyRole("Admin", "ADMIN")
                        // Allow other endpoints or authenticated where appropriate
                        .anyRequest().permitAll()
                )
                .addFilterBefore(operatorTokenAuthenticationFilter(), BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(nonOperatorBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    private BearerTokenResolver nonOperatorBearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> {
            String token = delegate.resolve(request);
            if (token != null && token.regionMatches(true, 0, "wpt_", 0, 4)) {
                return null;
            }
            return token;
        };
    }

    private OncePerRequestFilter operatorTokenAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    String opHeader = request.getHeader(OperatorTokenAuthenticationHandler.HEADER_NAME);
                    String authHeader = request.getHeader("Authorization");
                    var result = operatorTokenHandler.authenticate(opHeader, authHeader);
                    if (result instanceof OperatorTokenAuthenticationHandler.AuthenticateResult.Success s) {
                        ClaimsPrincipal principal = s.principal();
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        for (ClaimsPrincipal.Claim claim : principal.getClaims()) {
                            if ("scope".equalsIgnoreCase(claim.type())) {
                                authorities.add(new SimpleGrantedAuthority("SCOPE_" + claim.value()));
                                if ("*".equals(claim.value())) {
                                    authorities.add(new SimpleGrantedAuthority("ROLE_Admin"));
                                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                                    authorities.add(new SimpleGrantedAuthority("Admin"));
                                }
                            }
                        }
                        String name = principal.findFirst(ClaimsPrincipal.CLAIM_NAME_IDENTIFIER).orElse("operator");
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                name,
                                null,
                                authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("https://*.worldpinball.local", "http://*.worldpinball.local", "https://worldpinball.local", "http://worldpinball.local", "http://localhost:*", "https://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    /**
     * Extracts Keycloak realm roles mapped in "roles" or "realm_access.roles"
     * and maps them as Spring Security GrantedAuthority (ROLE_Admin, ROLE_ADMIN, Admin).
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // 1. Check direct "roles" claim (custom OIDC realm-roles mapper)
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof Collection<?> col) {
                for (Object r : col) {
                    if (r != null) {
                        String role = r.toString();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            }

            // 2. Check "realm_access.roles" (Keycloak standard claim)
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object rRoles = realmAccess.get("roles");
                if (rRoles instanceof Collection<?> col) {
                    for (Object r : col) {
                        if (r != null) {
                            String role = r.toString();
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                            authorities.add(new SimpleGrantedAuthority(role));
                        }
                    }
                }
            }

            return authorities;
        }
    }
}
