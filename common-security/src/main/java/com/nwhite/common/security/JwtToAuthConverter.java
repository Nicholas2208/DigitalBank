package com.nwhite.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * JwtToAuthConverter is responsible for converting a validated {@link Jwt} access token
 * into a Spring Security {@link AbstractAuthenticationToken} with proper authorities
 *
 * <p>Spring Security uses these authorities to enforce method-level access control,
 * such as with {@code @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')"}.</p>
 *
 * <p>This converter reads two sources of authority information from the token:</p>
 * <ul>
 *     <li><b>permissions</b> an array claim, added by Auth0 when RBAC is enabled</li>
 *     <li><b>scope</b> - a space-delimited string of OAuth2 scopes</li>
 * </ul>
 *
 */
@Component
public class JwtToAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    /**
     * Converts a decoded JWT into a Spring {@link JwtAuthenticationToken} with authorities.
     * This is automatically invoked by Spring Security during request processing.
     *
     * @param jwt the validated JWT access token from Auth0
     * @return a {@link JwtAuthenticationToken} containing authorities, derived from permissions and scopes
     * Spring Security Authorities
     * SCOPE_fdx:accounts.read
     * SCOPE_fdx:transactions.write
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> scopes = new LinkedHashSet<>();
        // 1. Extract permissions claim (Auth0 adds it when RBAC is enabled
        Object perms = jwt.getClaims().get("permissions");
        if (perms instanceof Collection<?>) {
            for (Object p : (Collection<?>) perms) {
                scopes.add(String.valueOf(p));
            }
        }
        // 2. Extract OAuth2 scope claim (space-delimited)
        String scope = jwt.getClaim("scope");
        if (scope != null && !scope.isBlank()) {
            // Split the space-delimited scopes and add each one
            scopes.addAll(Arrays.asList(scope.split(" ")));
        }
        // 3. Convert all collected scopes/permissions into GrantedAuthority objects
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (final String s : scopes) {
            authorities.add(() -> "SCOPE_" + s);
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
