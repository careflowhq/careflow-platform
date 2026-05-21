# Domain Boundaries

CareFlow uses **bounded contexts** aligned with microservices. Each service owns its data and business rules.

## Context map

```mermaid
flowchart TB
    subgraph edge [Edge]
        GW[API Gateway]
    end

    subgraph identity [Identity & Access]
        AUTH[auth-service]
    end

    subgraph tenant [Tenant]
        CLINIC[clinic-service]
    end

    subgraph clinical [Clinical Operations]
        PATIENT[patient-service]
    end

    subgraph future [Planned]
        FOLLOWUP[followup-service]
        NOTIFY[notification-service]
    end

    GW --> AUTH
    GW --> CLINIC
    GW --> PATIENT
    AUTH -->|onboard clinic| CLINIC
    PATIENT -.->|clinicId reference| CLINIC
    FOLLOWUP -.-> PATIENT
    NOTIFY -.-> FOLLOWUP
```

## Boundaries

| Domain | Service | Owns | Does NOT own |
|--------|---------|------|--------------|
| **Identity & Access** | auth-service | Users, credentials, JWT issuance | Clinics, patients, follow-ups |
| **Tenant** | clinic-service | Clinic aggregate, subscription plan | Users, patients |
| **Patient Management** | patient-service | Patient records per clinic | User auth, clinic config |
| **Follow-Up** (planned) | followup-service | Reminders, schedules, alerts | Patient CRUD, auth |
| **Notifications** (planned) | notification-service | Delivery (WhatsApp, etc.) | Business rules for follow-ups |

## Cross-boundary rules

1. **No shared database** — each service has its own PostgreSQL instance/schema.
2. **No foreign keys across services** — references use UUIDs (`clinicId`, `patientId`).
3. **Gateway validates JWT** — downstream services trust propagated headers, not raw tokens.
4. **Tenant isolation** — business data filtered by `clinicId` at application layer.
5. **Onboarding orchestration** — auth-service calls clinic-service internal API; no duplicate tenant creation in clients.

## Integration styles

| From → To | Style | Example |
|-----------|-------|---------|
| Client → Gateway | Sync REST | `POST /api/auth/login` |
| Gateway → Service | Sync REST + headers | `X-Clinic-Id` propagation |
| auth → clinic | Sync REST (internal) | `POST /internal/clinics` |
| followup → notification (future) | Async (RabbitMQ) | Appointment reminder event |

## Anti-patterns to avoid

- Patient service validating JWT directly (gateway responsibility)
- Client sending `clinicId` in request body for tenant scoping
- Shared `users` table across services
- Clinic creation only in auth-service without clinic-service record
