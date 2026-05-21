package com.careflow.clinicservice.security;

import com.careflow.clinicservice.exception.AccessDeniedException;
import com.careflow.clinicservice.tenant.TenantContext;

import java.util.UUID;

/**
 * Ownership rules for top-level clinic tenants.
 */
public final class ClinicAccessGuard {

    private ClinicAccessGuard() {
    }

    public static void requirePlatformAdmin() {
        if (!TenantContext.isPlatformAdmin()) {
            throw new AccessDeniedException("Platform admin role required");
        }
    }

    public static void requireOwnClinicOrPlatformAdmin(UUID clinicId) {
        if (TenantContext.isPlatformAdmin()) {
            return;
        }

        if (!TenantContext.clinicId().equals(clinicId)) {
            throw new AccessDeniedException("Access denied for clinic: " + clinicId);
        }
    }

    public static void requireClinicAdminOrPlatformAdmin(UUID clinicId) {
        if (TenantContext.isPlatformAdmin()) {
            return;
        }

        if (!"CLINIC_ADMIN".equals(TenantContext.role())) {
            throw new AccessDeniedException("Clinic admin role required");
        }

        requireOwnClinicOrPlatformAdmin(clinicId);
    }
}
