# User Entity

Owned by **auth-service** (`auth_db.users`).

## Fields

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | Primary key |
| clinicId | UUID | Tenant boundary — set at creation, never from client on clinical APIs |
| fullName | String | Display name |
| email | String | Unique |
| passwordHash | String | BCrypt |
| role | UserRole | Enum |

## Role enum

| Role | Creation path |
|------|---------------|
| `PLATFORM_ADMIN` | Manual seed (future) |
| `CLINIC_ADMIN` | Self-register → creates clinic |
| `DOCTOR` | Invitation + register-invite |
| `ASSISTANT` | Invitation + register-invite |

## Relationships

- belongs to one clinic (`clinicId`)
- issues JWT with `userId`, `clinicId`, `role`
- CLINIC_ADMIN can invite DOCTOR / ASSISTANT

## Related

- [Invitation](./invitation.md)
- [Clinic](./clinic.md)
