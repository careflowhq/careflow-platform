# Architecture Decision Records (ADR)

Formal log of significant architectural decisions for CareFlow.

## Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0001](./0001-use-microservices.md) | Use pragmatic microservices | Accepted | — |
| [0002](./0002-multi-tenant-architecture.md) | Multi-tenant architecture (clinicId) | Accepted | 2026-05-20 |
| [0003](./0003-rabbitmq-for-notifications.md) | RabbitMQ for async notifications | Accepted | 2026-05-20 |
| [0004](./0004-monorepo-strategy.md) | Monorepo strategy | Accepted | — |

## ADR lifecycle

| Status | Meaning |
|--------|---------|
| **Proposed** | Under discussion |
| **Accepted** | Active decision |
| **Deprecated** | Superseded, kept for history |
| **Superseded** | Replaced by newer ADR (link it) |

## How to add a new ADR

1. Copy the next number: `0005-short-title.md`
2. Use sections: **Status**, **Context**, **Decision**, **Consequences**
3. Add row to this index
4. Link from [progress log](../progress/progress-log.md) if implementation follows

## Template

```markdown
# ADR NNNN - Title

## Status
Proposed | Accepted | Deprecated | Superseded by ADR-XXXX

## Context
What is the issue?

## Decision
What was decided?

## Consequences
### Positive
### Negative
```

## Related

- [Domain boundaries](../architecture/domain-boundaries.md)
- [Service ownership](../architecture/service-ownership.md)
- [Multitenancy](../architecture/multitenancy.md)
