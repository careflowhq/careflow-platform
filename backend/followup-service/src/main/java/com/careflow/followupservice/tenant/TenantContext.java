package com.careflow.followupservice.tenant;

import com.careflow.followupservice.exception.MissingTenantContextException;

import java.util.UUID;

/**
 * Request-scoped tenant identity derived from gateway-propagated headers.
 *
 * Tenant isolation strategy:
 * - clinicId defines the tenant boundary for every query and command
 * - userId is used as createdBy audit field and for future authorization rules
 * - role is available for future role-based follow-up actions
 * - clinicId is never accepted from request payloads
 */
public final class TenantContext {

    private static final ThreadLocal<TenantIdentity> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantIdentity identity) {
        CURRENT.set(identity);
    }

    public static TenantIdentity get() {
        TenantIdentity identity = CURRENT.get();
        if (identity == null) {
            throw new MissingTenantContextException("Missing tenant identity headers");
        }
        return identity;
    }

    public static UUID clinicId() {
        return get().clinicId();
    }

    public static UUID userId() {
        return get().userId();
    }

    public static String role() {
        return get().role();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TenantIdentity(UUID userId, UUID clinicId, String role) {
    }
}
