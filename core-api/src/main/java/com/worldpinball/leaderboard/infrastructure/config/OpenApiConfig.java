package com.worldpinball.leaderboard.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${keycloak.auth-server-url:https://auth.worldpinball.local}")
    private String authServerUrl;

    @Value("${keycloak.realm:worldpinball}")
    private String realm;

    @Bean
    public OpenAPI customOpenAPI() {
        String realmUrl = authServerUrl + "/realms/" + realm;

        OAuthFlow authFlow = new OAuthFlow()
                .authorizationUrl(realmUrl + "/protocol/openid-connect/auth")
                .tokenUrl(realmUrl + "/protocol/openid-connect/token")
                .scopes(new Scopes()
                        .addString("openid", "OpenID Connect")
                        .addString("profile", "User profile")
                        .addString("email", "User email")
                        .addString("roles", "User roles"));

        SecurityScheme oauth2Scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("Keycloak OpenID Connect / OAuth2 PKCE (Client: worldpinball-swagger)")
                .flows(new OAuthFlows().authorizationCode(authFlow));

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer Token");

        return new OpenAPI()
                .info(new Info()
                        .title("WorldPinball Leaderboard API")
                        .version("v1.0")
                        .description("REST API for WorldPinball Leaderboard, Game Sessions, Matchmaking, Machine Provisioning and Telemetry.")
                        .contact(new Contact().name("WorldPinball Team"))
                        .license(new License().name("Apache 2.0")))
                .components(new Components()
                        .addSecuritySchemes("Keycloak_OAuth2", oauth2Scheme)
                        .addSecuritySchemes("Bearer_Token", bearerScheme))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Keycloak_OAuth2", List.of("openid", "profile", "roles")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer_Token"));
    }
}
