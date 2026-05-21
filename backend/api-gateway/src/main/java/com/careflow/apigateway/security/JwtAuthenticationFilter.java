package com.careflow.apigateway.security;

import com.careflow.apigateway.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class JwtAuthenticationFilter extends AuthenticationWebFilter {

    public JwtAuthenticationFilter(JwtService jwtService) {
        super(createAuthenticationManager(jwtService));
        setServerAuthenticationConverter(createAuthenticationConverter());
        setAuthenticationFailureHandler((exchange, ex) -> {
            exchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getExchange().getResponse().setComplete();
        });
    }

    private static ReactiveAuthenticationManager createAuthenticationManager(JwtService jwtService) {
        return authentication -> {
            String token = (String) authentication.getCredentials();
            try {
                JwtClaims claims = jwtService.validateAndExtractClaims(token);
                return Mono.just(new JwtAuthenticationToken(claims));
            } catch (JwtException ex) {
                return Mono.error(ex);
            }
        };
    }

    private static ServerAuthenticationConverter createAuthenticationConverter() {
        return (ServerWebExchange exchange) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.empty();
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                return Mono.empty();
            }

            return Mono.just(new BearerTokenAuthentication(token));
        };
    }

    private static final class BearerTokenAuthentication
            extends org.springframework.security.authentication.AbstractAuthenticationToken {

        private final String token;

        private BearerTokenAuthentication(String token) {
            super(null);
            this.token = token;
            setAuthenticated(false);
        }

        @Override
        public Object getCredentials() {
            return token;
        }

        @Override
        public Object getPrincipal() {
            return null;
        }
    }
}
