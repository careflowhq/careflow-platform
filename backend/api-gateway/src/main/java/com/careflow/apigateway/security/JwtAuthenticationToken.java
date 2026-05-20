package com.careflow.apigateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final JwtClaims claims;

    public JwtAuthenticationToken(JwtClaims claims) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public JwtClaims getPrincipal() {
        return claims;
    }
}
