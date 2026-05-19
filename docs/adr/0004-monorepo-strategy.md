# ADR 0004 - Use Monorepo Strategy

## Status
Accepted

## Context

CareFlow AI contains:
- multiple microservices
- shared documentation
- frontend applications
- infrastructure code

The project uses AI-assisted engineering and spec-driven development.

## Decision

Use a monorepo strategy for:
- shared visibility
- centralized specs
- easier local development
- improved AI context awareness

## Consequences

### Positive
- simpler coordination
- easier refactoring
- centralized documentation
- improved Cursor context

### Negative
- larger repository
- more shared responsibility