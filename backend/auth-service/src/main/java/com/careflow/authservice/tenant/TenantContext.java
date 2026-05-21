package com.careflow.authservice.tenant;

import com.careflow.authservice.exception.MissingTenantContextException;

import java.util.UUID;

/**
 * Request-scoped identity from gateway headers.
 * Used for staff invitation — clinicId comes from the inviter's JWT, not the request body.
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
