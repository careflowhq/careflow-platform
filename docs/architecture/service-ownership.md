# Service Ownership

Who owns what in the CareFlow monorepo.

## Ownership matrix

| Service | Team scope | Port | Database | Public API prefix | Owner responsibilities |
|---------|------------|------|----------|-------------------|------------------------|
| **api-gateway** | Platform | 8080 | — | `/health`, `/api/**` | Routing, JWT validation, identity propagation, public vs protected auth routes |
| **auth-service** | Identity | 8081 | `auth_db` | `/api/auth/**` | Register, login, JWT, invitations, user credentials |
| **clinic-service** | Tenant | 8083 | `clinic_db` | `/api/clinics/**` | Clinic lifecycle, tenant metadata |
| **patient-service** | Clinical | 8082 | `patient_db` | `/api/patients/**` | Patient CRUD, clinic-scoped data |
| **followup-service** | Clinical | 8084 | `followup_db` | `/api/followups/**` | Follow-up CRUD, overdue scheduler |
| **notification-service** | Platform (planned) | TBD | TBD | internal/events | Email, WhatsApp, delivery |

## Auth-service endpoints

| Endpoint | Gateway auth | Owner logic |
|----------|--------------|-------------|
| `POST /api/auth/login` | Public | AuthService |
| `POST /api/auth/register` | Public | AuthService + clinic onboard |
| `POST /api/auth/register-clinic` | Public | Alias register |
| `POST /api/auth/register-invite` | Public | InviteService.accept |
| `POST /api/auth/invite` | **JWT required** | InviteService.create |

## Code ownership (monorepo)

```
backend/
├── api-gateway/      → Platform / edge
├── auth-service/     → Identity domain (users + invitations)
├── clinic-service/   → Tenant domain
├── patient-service/  → Patient domain
└── followup-service/ → Follow-up domain

docs/
├── adr/              → Architecture decisions (shared)
├── architecture/     → System design (shared)
├── progress/         → Project memory (shared)
├── api/              → OpenAPI specs + Postman (domain owners)
└── data-model/       → Entity specs (domain owners)
```

## Decision authority

| Change type | Primary owner | Review |
|-------------|---------------|--------|
| New public endpoint | Owning service | Gateway route + docs/api |
| JWT claims / headers | api-gateway + auth-service | All downstream services |
| Staff invitation rules | auth-service | Product + gateway |
| Tenant model | clinic-service + architecture | auth, patient, followup |
| New ADR | Whoever proposes | Document in `docs/adr/` |

## Runtime dependencies

```mermaid
flowchart LR
    GW[api-gateway]
    AUTH[auth-service]
    CLINIC[clinic-service]
    PATIENT[patient-service]
    FOLLOWUP[followup-service]

    GW --> AUTH
    GW --> CLINIC
    GW --> PATIENT
    GW --> FOLLOWUP
    AUTH -->|CLINIC_ADMIN register| CLINIC
```

- **patient-service** / **followup-service** — headers only, no runtime calls to auth/clinic.
- **auth-service** calls clinic-service only during CLINIC_ADMIN registration.
- **Invitations** are fully internal to auth-service (no clinic-service call).

## On-call / ops (future)

For MVP, all services deploy together via Docker Compose on a single VPS (see ADR 0001).
