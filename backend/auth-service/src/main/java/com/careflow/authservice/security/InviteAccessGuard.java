package com.careflow.authservice.security;

import com.careflow.authservice.entity.UserRole;
import com.careflow.authservice.exception.AccessDeniedException;
import com.careflow.authservice.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Only clinic owners/admins can invite staff to their tenant.
 */
@Component
public class InviteAccessGuard {

    public void assertCanInvite() {
        String role = TenantContext.role();
        if (!UserRole.CLINIC_ADMIN.name().equals(role)) {
            throw new AccessDeniedException("Only CLINIC_ADMIN can invite staff");
        }
    }

    public void assertInvitableRole(UserRole role) {
        if (role != UserRole.DOCTOR && role != UserRole.ASSISTANT) {
            throw new AccessDeniedException("Only DOCTOR and ASSISTANT roles can be invited");
        }
    }
}
