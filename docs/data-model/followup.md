# FollowUp Entity

Owned by **followup-service** (`followup_db.followups`).

## Fields

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | Primary key |
| clinicId | UUID | Tenant boundary (from header) |
| patientId | UUID | Reference to patient-service |
| doctorId | UUID | Optional assigned doctor |
| type | String | e.g. POST_CONSULTATION |
| scheduledDate | Instant | When follow-up is due |
| status | FollowUpStatus | Lifecycle |
| notes | String | Optional |
| createdBy | UUID | From X-User-Id |
| createdAt | Instant | Auto-set |

## Status enum

| Status | Meaning |
|--------|---------|
| `PENDING` | Active, awaiting action |
| `COMPLETED` | Manually completed |
| `MISSED` | Past scheduledDate (scheduler) |
| `CANCELLED` | Cancelled by user |

## API (gateway)

| Method | Path |
|--------|------|
| POST | `/api/followups` |
| GET | `/api/followups` |
| GET | `/api/followups/pending` |
| GET | `/api/followups/{id}` |
| PUT | `/api/followups/{id}` |
| PATCH | `/api/followups/{id}/complete` |
| DELETE | `/api/followups/{id}` (→ CANCELLED) |

## Scheduler

Cron default: every 15 min — marks overdue `PENDING` as `MISSED`.

## Related

- [Patient](./patient.md)
