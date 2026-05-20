package com.careflow.apigateway.filter;

import com.careflow.apigateway.security.JwtAuthenticationToken;
import com.careflow.apigateway.security.JwtClaims;
import com.careflow.apigateway.security.TenantIdentityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Propagates authenticated JWT claims to downstream microservices as trusted headers.
 *
 * Flow:
 * 1. JwtAuthenticationFilter validates the Bearer token and stores JwtClaims in the security context.
 * 2. This filter reads those claims from ReactiveSecurityContextHolder.
 * 3. Any client-supplied identity headers are removed to prevent spoofing.
 * 4. Gateway-trusted headers are injected before the request is routed downstream.
 */
@Component
public class TenantIdentityPropagationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    if (!(securityContext.getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
                        return chain.filter(stripClientIdentityHeaders(exchange));
                    }

                    JwtClaims claims = jwtAuth.getPrincipal();
                    ServerWebExchange enrichedExchange = exchange.mutate()
                            .request(buildDownstreamRequest(exchange.getRequest(), claims))
                            .build();

                    return chain.filter(enrichedExchange);
                })
                .switchIfEmpty(chain.filter(stripClientIdentityHeaders(exchange)));
    }

    private ServerHttpRequest buildDownstreamRequest(ServerHttpRequest request, JwtClaims claims) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(TenantIdentityHeaders.USER_ID);
                    headers.remove(TenantIdentityHeaders.CLINIC_ID);
                    headers.remove(TenantIdentityHeaders.ROLE);
                    headers.set(TenantIdentityHeaders.USER_ID, claims.userId().toString());
                    headers.set(TenantIdentityHeaders.CLINIC_ID, claims.clinicId().toString());
                    headers.set(TenantIdentityHeaders.ROLE, claims.role());
                })
                .build();
    }

    private ServerWebExchange stripClientIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(TenantIdentityHeaders.USER_ID);
                    headers.remove(TenantIdentityHeaders.CLINIC_ID);
                    headers.remove(TenantIdentityHeaders.ROLE);
                })
                .build();

        return exchange.mutate().request(sanitizedRequest).build();
    }

    @Override
    public int getOrder() {
        // Runs after JWT authentication and before the request is forwarded downstream.
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
