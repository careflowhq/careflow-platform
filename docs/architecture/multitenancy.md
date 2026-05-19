# Multi-Tenancy Architecture

## Tenant Model

A clinic represents a tenant.

All business data belongs to a clinic.

## Isolation Strategy

Shared application architecture with tenant isolation using clinicId.

## JWT Claims

JWT tokens must contain:
- userId
- clinicId
- role

## Roles

- PLATFORM_ADMIN
- CLINIC_ADMIN
- DOCTOR
- ASSISTANT

## Security Rules

Users can only access data belonging to their clinic.

All services must validate:
- clinic ownership
- role permissions

## Database Strategy

Each microservice owns its own database.

Tenant isolation is implemented at the application layer using clinicId.