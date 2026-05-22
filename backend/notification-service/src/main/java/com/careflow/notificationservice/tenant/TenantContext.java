package com.careflow.notificationservice.tenant;

import com.careflow.notificationservice.exception.MissingTenantContextException;

import java.util.UUID;

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

    public static void clear() {
        CURRENT.remove();
    }

    public record TenantIdentity(UUID userId, UUID clinicId, String role) {
    }
}
