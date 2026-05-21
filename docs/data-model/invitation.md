# Invitation Entity

Owned by **auth-service** (`auth_db.invitations`).

Pending staff onboarding for an existing clinic tenant.

## Fields

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | Primary key |
| token | String | Unique invite token (UUID string) |
| clinicId | UUID | From inviter JWT — not from request body |
| email | String | Invitee email |
| fullName | String | Invitee name |
| role | UserRole | `DOCTOR` or `ASSISTANT` only |
| invitedBy | UUID | Admin user id |
| status | InvitationStatus | Lifecycle state |
| expiresAt | Instant | Default 7 days (`careflow.invite.expiration-days`) |
| createdAt | Instant | Auto-set |

## Status enum

| Status | Meaning |
|--------|---------|
| `PENDING` | Awaiting register-invite |
| `ACCEPTED` | User created |
| `EXPIRED` | Past expiresAt |

## API

| Action | Endpoint |
|--------|----------|
| Create | `POST /api/auth/invite` (CLINIC_ADMIN + JWT) |
| Accept | `POST /api/auth/register-invite` (public) |

## Related

- [User](./user.md)
