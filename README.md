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
| **Frontend** | [frontend/README.md](frontend/README.md) |
| **Demo local** | [docs/demo/local-demo.md](docs/demo/local-demo.md) |
| **Notification events** | [docs/api/notification-events.md](docs/api/notification-events.md) |

## Status

MVP **backend + frontend** en local: auth, clinic, patient, followup, invitaciones staff, web app Next.js (UI en español).  
**Demo:** [guía local](docs/demo/local-demo.md) · Estado: [progress log](docs/progress/progress-log.md)

## Local run

### Backend

```bash
cp .env.example .env   # optional
cd infra/docker && docker compose up -d
# Start: api-gateway, auth-service, clinic-service, patient-service, followup-service, notification-service
```

### Frontend

```bash
cd frontend && npm install && npm run dev
# http://localhost:3000
```

**Environment variables** (see [.env.example](.env.example)):

| Variable | Used by | Purpose |
|----------|---------|---------|
| `CAREFLOW_JWT_SECRET` | api-gateway, auth-service | Sign/validate JWT (min 32 chars) |
| `CAREFLOW_INTERNAL_API_KEY` | auth-service, clinic-service | Service-to-service `/internal/**` |

**Smoke tests:** import [docs/api/careflow-smoke.postman_collection.json](docs/api/careflow-smoke.postman_collection.json) into Postman.

Details: [docs/progress/progress-log.md](docs/progress/progress-log.md)
