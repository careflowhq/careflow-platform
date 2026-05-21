package com.careflow.clinicservice.tenant;

import com.careflow.clinicservice.exception.MissingTenantContextException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            TenantContext.set(parseIdentity(request));
            filterChain.doFilter(request, response);
        } catch (MissingTenantContextException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"" + ex.getMessage() + "\"}");
        } finally {
            TenantContext.clear();
        }
    }

    private TenantContext.TenantIdentity parseIdentity(HttpServletRequest request) {
        String userId = request.getHeader(TenantHeaders.USER_ID);
        String clinicId = request.getHeader(TenantHeaders.CLINIC_ID);
        String role = request.getHeader(TenantHeaders.ROLE);

        if (isBlank(userId) || isBlank(clinicId) || isBlank(role)) {
            throw new MissingTenantContextException("Missing required tenant identity headers");
        }

        try {
            return new TenantContext.TenantIdentity(
                    UUID.fromString(userId),
                    UUID.fromString(clinicId),
                    role
            );
        } catch (IllegalArgumentException ex) {
            throw new MissingTenantContextException("Invalid tenant identity header format");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
