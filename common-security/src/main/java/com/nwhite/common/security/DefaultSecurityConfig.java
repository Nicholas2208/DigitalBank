package com.nwhite.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Default security configuration, that makes this Spring Boot microservice
 * an OAuth2 Resource Server
 *
 * <p>This class ensures, that every incoming request with a Bearer token is:</p>
 * <ul>
 *     <li>Verified against OAuth0's JWKS public keys (RS256 signature verification)</li>
 *     <li>Checked for correct issuer (iss claim matches your Auth0 tenant)</li>
 *     <li>Checked for correct audience (aud claim contains your API identifier)</li>
 *     <li>Converted into Spring Security authorities (so @PreAuthorize annotations work)</li>
 * </ul>
 *
 * <p>By doing this inside each microservice, we follow a <b>zero-trust</b> model -
 * every service independently validates tokens and does not rely on a gateway to do that.</p>
 */
@Configuration
@EnableMethodSecurity
public class DefaultSecurityConfig {
    // Auth0 tenant issuer-uri
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;
    // The audience (API identifier) configured in Auth0
    @Value("${auth0.audience}")
    private String audience;
    // Custom converter, that maps JWT claims (permissions/scope) to Spring authorities
    private final JwtToAuthConverter jwtToAuthConverter;

    public DefaultSecurityConfig(JwtToAuthConverter jwtToAuthConverter) {
        this.jwtToAuthConverter = jwtToAuthConverter;
    }

    /**
     * Defines the Spring Security filter chain
     * This method configures the core security behavior for the microservice:
     * - Disable CSRF (no need for stateless REST APIs)
     * - Make the service stateless (no HTTP sessions)
     * - Define which endpoints are public vs protected
     * - Enable OAuth2 Resource Server mode to validate JWT access tokens
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http    // Disable CSRF since we're using stateless JWTs and not browser sessions
                .csrf(cs -> cs.disable())
                // Ensure the service is stateless - no JSESSIONID cookies or server-side sessions
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorisation rules for HTTP requests
                .authorizeHttpRequests(auth -> auth
                        // Public endpoinds - no JWT required
                        .requestMatchers(
                                "/actuator/health",          // Health check for monitoring
                                "/api/v1/health",            // API-specific health endpoint
                                "/api/v1/customer/register", // Public endpoint to register new customers
                                "/.well-known/jwks.json"     // JWKS public key endpoint (used by Auth0)
                        ).permitAll()
                        // Everything else requires a valid JWT access token
                        .anyRequest().authenticated()
                )
                // Enable OAuth2 Resource Server mode with JWT validation
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                // Configure how tokens are decoded and verified
                                .decoder(jwtDecoder())
                                // Convert JWT claims (permissions, scopes) into Spring authorities
                                .jwtAuthenticationConverter(jwtToAuthConverter)
                        )
                );
        return http.build();
    }

    /**
     *
     * Configures a {@link JwtDecoder} that:
     * - Downloads the JWKS (public keys) from Auth0
     * - Verifies the RS256 signature of incoming tokens
     * - Validates, that the token's issuer (iss) matches your Auth0 tenant
     * - Validates, that the token's audience (aud) includes your API identifier
     *
     * @return a configured {@link JwtDecoder}
     */
    public JwtDecoder jwtDecoder() {
        // Build a NimbusJwtDecoder using the issuer URL (it will auto-discover JWKS keys)
        NimbusJwtDecoder dec = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);
        // Validator 1: Ensure the issuer (iss) claim matches your tenant
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        // Validator 2: Ensure the audience (aud) claim contains our API identifier
        OAuth2TokenValidator<Jwt> withAudience = token -> {
            Object aud = token.getClaims().get("aud");
            if (aud instanceof List && ((List<?>) aud).contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            // Reject tokens missing or having the wrong audience
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid token", "missing/invalid audience", null));
        };
        // Combine both validators (issuer AND audience must pass)
        dec.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

        return dec;
    };
}
