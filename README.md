# CareFlow Platform

Multi-tenant healthcare SaaS platform focused on patient follow-up, retention, and chronic care management.

## Vision

CareFlow helps private clinics automate patient engagement workflows through intelligent follow-up and communication systems.

## Architecture

- Microservices (Spring Boot 3, Java 21)
- API Gateway with JWT + tenant header propagation
- PostgreSQL (database per service)
- RabbitMQ (async notifications, planned)
- Docker Compose (local / MVP deploy)

## Documentation

| Area | Path |
|------|------|
| **Progress / project memory** | [docs/progress/](docs/progress/) |
| **ADRs** | [docs/adr/](docs/adr/) |
| **Architecture** | [docs/architecture/](docs/architecture/) |
| **Domain boundaries** | [docs/architecture/domain-boundaries.md](docs/architecture/domain-boundaries.md) |
| **Service ownership** | [docs/architecture/service-ownership.md](docs/architecture/service-ownership.md) |
| **Platform diagrams** | [docs/architecture/platform-diagram.md](docs/architecture/platform-diagram.md) |
| **Product** | [docs/product/](docs/product/) |
| **API specs** | [docs/api/](docs/api/) |

## Status

MVP backend operational: **auth**, **clinic**, **patient**, **followup**, multi-tenant isolation, staff invitations (DOCTOR/ASSISTANT). See [progress log](docs/progress/progress-log.md).

**Branches:** `main` includes hardening + followup. Staff invitations en `feat/staff-invitations` (pendiente merge).

## Local run

```bash
cp .env.example .env   # optional — defaults work for local dev
cd infra/docker && docker compose up -d
# Start: api-gateway, auth-service, clinic-service, patient-service, followup-service
```

**Environment variables** (see [.env.example](.env.example)):

| Variable | Used by | Purpose |
|----------|---------|---------|
| `CAREFLOW_JWT_SECRET` | api-gateway, auth-service | Sign/validate JWT (min 32 chars) |
| `CAREFLOW_INTERNAL_API_KEY` | auth-service, clinic-service | Service-to-service `/internal/**` |

**Smoke tests:** import [docs/api/careflow-smoke.postman_collection.json](docs/api/careflow-smoke.postman_collection.json) into Postman.

Details: [docs/progress/progress-log.md](docs/progress/progress-log.md)
