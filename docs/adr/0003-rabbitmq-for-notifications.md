# ADR 0003 - RabbitMQ for Async Notifications

## Status
Accepted

## Context

CareFlow will send appointment reminders, post-consultation follow-ups, and inactivity alerts (see [followups.md](../business-rules/followups.md)). Notification delivery (e.g. WhatsApp) must not block HTTP request paths and may require retries.

## Decision

Use **RabbitMQ** for asynchronous event-driven communication between:

- **followup-service** (orchestration, business rules)
- **notification-service** (delivery channels)

Synchronous REST remains for client-facing APIs and service queries. RabbitMQ is provisioned in `infra/docker/` from MVP phase 1 but consumed when notification-service is implemented.

## Consequences

### Positive
- Decouples follow-up logic from delivery providers
- Supports retries and future scaling of notification workers
- Aligns with pragmatic microservices (ADR 0001)

### Negative
- Additional operational component
- Event schema and idempotency must be designed
- Not needed until Phase 3 — infra ready early

## Notes

- Do not introduce Kafka or service mesh at this stage
- Define event contracts in `docs/api/` when notification-service starts
