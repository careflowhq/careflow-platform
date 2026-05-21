# Platform Diagram

High-level runtime architecture (MVP phase).

## Request flow — authenticated

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant S as Microservice
    participant DB as PostgreSQL

    C->>G: Request + Bearer JWT
    G->>G: Validate JWT
    G->>G: Inject X-User-Id, X-Clinic-Id, X-Role
    G->>S: Proxied request
    S->>S: TenantContext / business rules
    S->>DB: Query scoped by clinicId
    DB-->>S: Data
    S-->>G: Response
    G-->>C: Response
```

## Request flow — CLINIC_ADMIN registration

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant A as auth-service
    participant CL as clinic-service
    participant DB1 as auth_db
    participant DB2 as clinic_db

    C->>G: POST /api/auth/register
    G->>A: POST /auth/register
    A->>CL: POST /internal/clinics
    CL->>DB2: INSERT clinic
    CL-->>A: clinicId
    A->>DB1: INSERT user (CLINIC_ADMIN)
    A-->>C: 201 Created
```

## Request flow — staff invitation

```mermaid
sequenceDiagram
    participant Admin as CLINIC_ADMIN
    participant G as API Gateway
    participant A as auth-service
    participant Staff as Invited user
    participant DB as auth_db

    Admin->>G: POST /api/auth/invite + JWT
    G->>G: Validate JWT, inject headers
    G->>A: POST /auth/invite
    A->>DB: INSERT invitation (PENDING)
    A-->>Admin: 201 { token, expiresAt }

    Note over Admin,Staff: Token shared manually (WhatsApp) until notification-service

    Staff->>G: POST /api/auth/register-invite
    G->>A: POST /auth/register-invite
    A->>DB: INSERT user (DOCTOR|ASSISTANT)
    A->>DB: UPDATE invitation (ACCEPTED)
    A-->>Staff: 201 Created
    Staff->>G: POST /api/auth/login
    G-->>Staff: JWT (same clinicId)
```

## Deployment view (local / MVP)

```mermaid
flowchart TB
    subgraph client [Client]
        POSTMAN[Postman / Frontend]
    end

    subgraph backend [Backend - localhost]
        GW[api-gateway :8080]
        AUTH[auth-service :8081]
        PAT[patient-service :8082]
        CLIN[clinic-service :8083]
        FU[followup-service :8084]
    end

    subgraph infra [infra/docker]
        PA[(postgres-auth :5433)]
        PP[(postgres-patient :5434)]
        PC[(postgres-clinic :5435)]
        PF[(postgres-followup :5436)]
        RMQ[RabbitMQ :5672]
    end

    POSTMAN --> GW
    GW --> AUTH
    GW --> PAT
    GW --> CLIN
    GW --> FU
    AUTH --> PA
    AUTH --> CLIN
    PAT --> PP
    CLIN --> PC
    FU --> PF
```

## Security layers

| Layer | Responsibility |
|-------|----------------|
| API Gateway | JWT validation, granular public vs protected auth routes |
| Gateway filter | Identity header propagation, anti-spoofing |
| auth TenantContextFilter | Headers required for `/auth/invite` only |
| Downstream tenant filter | Require trusted headers on all clinical routes |
| Service layer | Tenant isolation, role checks (invite guard) |
| Internal API | `X-Internal-Api-Key` for auth → clinic onboarding |

## Gateway — public vs protected auth routes

| Route | Auth |
|-------|------|
| `/api/auth/login` | Public |
| `/api/auth/register` | Public |
| `/api/auth/register-clinic` | Public |
| `/api/auth/register-invite` | Public |
| `/api/auth/invite` | JWT required |
| `/api/patients/**`, etc. | JWT required |
