package com.careflow.followupservice.tenant;

/**
 * Header names injected by the API Gateway after JWT validation.
 * Downstream services must treat these as trusted identity signals.
 */
public final class TenantHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String CLINIC_ID = "X-Clinic-Id";
    public static final String ROLE = "X-Role";

    private TenantHeaders() {
    }
}
