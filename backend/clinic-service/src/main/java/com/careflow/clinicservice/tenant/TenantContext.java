package com.careflow.clinicservice.tenant;

import com.careflow.clinicservice.exception.MissingTenantContextException;

import java.util.UUID;

/**
 * Request-scoped identity propagated by the API Gateway.
 *
 * Clinics are top-level tenants. This context drives ownership checks:
 * - PLATFORM_ADMIN manages all clinics
 * - CLINIC_ADMIN manages only the clinic matching X-Clinic-Id
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

    public static boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(get().role());
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TenantIdentity(UUID userId, UUID clinicId, String role) {
    }
}
