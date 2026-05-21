# ADR 0002 - Multi-Tenant Architecture

## Status
Accepted

## Context

CareFlow serves multiple private clinics (tenants) on shared infrastructure. Patient data and clinic operations must be isolated. Users belong to one clinic and access data only within that tenant boundary.

## Decision

Adopt **shared application, shared database per service** with **application-layer isolation** using `clinicId`:

- **Tenant = Clinic** (top-level aggregate owned by clinic-service)
- JWT must contain: `userId`, `clinicId`, `role`
- API Gateway validates JWT and propagates identity as trusted headers
- Downstream services scope all queries by `clinicId` from headers (never from client body)
- CLINIC_ADMIN registration creates a real clinic in clinic-service before user creation

## Consequences

### Positive
- Simple MVP deployment (no schema-per-tenant)
- Consistent tenant ID across auth, clinic, patient services
- Clear security boundary at gateway + service layer

### Negative
- Requires discipline: every query must filter by `clinicId`
- Bug in scoping could leak cross-tenant data
- Platform admin flows need explicit role checks

## Related

- [multitenancy.md](../architecture/multitenancy.md)
- [domain-boundaries.md](../architecture/domain-boundaries.md)
