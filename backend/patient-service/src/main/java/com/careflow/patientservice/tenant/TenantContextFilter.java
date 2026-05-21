package com.careflow.patientservice.tenant;

import com.careflow.patientservice.exception.MissingTenantContextException;
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

/**
 * Reads trusted identity headers from the API Gateway and binds them to TenantContext.
 *
 * Flow:
 * 1. Gateway validates JWT and injects X-User-Id, X-Clinic-Id, X-Role
 * 2. This filter extracts and validates those headers for patient endpoints
 * 3. Service layer uses TenantContext.clinicId() to enforce tenant isolation
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

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

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
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
