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
    A->>DB1: INSERT user
    A-->>C: 201 Created
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
    end

    subgraph infra [infra/docker]
        PA[(postgres-auth :5433)]
        PP[(postgres-patient :5434)]
        PC[(postgres-clinic :5435)]
        RMQ[RabbitMQ :5672]
    end

    POSTMAN --> GW
    GW --> AUTH
    GW --> PAT
    GW --> CLIN
    AUTH --> PA
    AUTH --> CLIN
    PAT --> PP
    CLIN --> PC
```

## Security layers

| Layer | Responsibility |
|-------|----------------|
| API Gateway | JWT validation, public vs protected routes |
| Gateway filter | Identity header propagation, anti-spoofing |
| Downstream filter | Require trusted headers |
| Service layer | Tenant isolation, role checks |
| Internal API | `X-Internal-Api-Key` for auth → clinic onboarding |
