# CareFlow Progress Log

Documento vivo de **project memory** para sincronizar estado entre sesiones (Cursor, ChatGPT, GitHub).

> Actualizar después de cada implementación relevante.  
> Índice: [README.md](./README.md)

---

## Última actualización

**Fecha:** 2026-05-20  
**Sesión:** Integración auth register → clinic-service + reorganización docs

---

## Estado general

CareFlow es una plataforma SaaS multi-tenant para clínicas de salud. El proyecto supera la fase tutorial: infraestructura real, auth distribuida end-to-end, tenants reales y primer dominio clínico (patients).

**Documentación institucional:**

| Doc | Ubicación |
|-----|-----------|
| Progress log | `docs/progress/progress-log.md` |
| ADRs | `docs/adr/` |
| Domain boundaries | `docs/architecture/domain-boundaries.md` |
| Service ownership | `docs/architecture/service-ownership.md` |
| Platform diagram | `docs/architecture/platform-diagram.md` |

---

## Infraestructura

| Componente | Estado | Notas |
|------------|--------|-------|
| Docker Compose | ✅ | PostgreSQL + RabbitMQ en `infra/docker/` |
| PostgreSQL | ✅ | `auth_db` (5433), `patient_db` (5434), `clinic_db` (5435), `followup_db` (5436) |
| RabbitMQ | ✅ | Preparado para notificaciones (fase futura) |

---

## Backend — Servicios implementados

### API Gateway (8080)

JWT validation, propagación `X-User-Id` / `X-Clinic-Id` / `X-Role`, proxy a auth/patient/clinic.

### Auth Service (8081)

Register + login, BCrypt, JWT. **Register CLINIC_ADMIN** crea clínica real vía `POST /internal/clinics` en clinic-service.

### Clinic Service (8083)

CRUD clínicas (tenant raíz), ownership por rol, onboarding interno para auth-service.

### Patient Service (8082)

CRUD pacientes con aislamiento por `clinicId` del header propagado.

### FollowUp Service (8084)

CRUD follow-ups, listado pending, complete/cancel, scheduler overdue → MISSED. BD `followup_db` (5436).

---

## Flujo onboarding (actual)

```
POST /api/auth/register (CLINIC_ADMIN + clinicName/country/timezone)
  → auth-service
  → clinic-service POST /internal/clinics
  → user creado con clinicId real
  → login → JWT coherente
  → GET /api/clinics / POST /api/patients funcionan sin SQL manual
```

---

## Issues resueltos

| Issue | Fix |
|-------|-----|
| 403 en `/api/auth/**` | `StripPrefix=1` en gateway |
| 500 JWT clinicId | Parser String → UUID en gateway |
| 404 GET /api/clinics | Reiniciar gateway + clínica en clinic_db |
| clinicId random | Integración register → clinic-service |

---

## Próximos pasos

- [ ] Tests integración gateway → servicios
- [ ] Exception handler auth (409 email duplicado, 401 login)
- [ ] Invitación DOCTOR/ASSISTANT a clínica existente
- [ ] Notification Service (RabbitMQ)
- [ ] Externalizar secrets (JWT, internal API key)

---

## Historial de implementaciones

| Fecha | Implementación | Servicio |
|-------|----------------|----------|
| 2026-05-19 | JWT validation stateless | api-gateway |
| 2026-05-19 | StripPrefix auth routes | api-gateway |
| 2026-05-19 | Propagación headers identidad | api-gateway |
| 2026-05-20 | Patient Service MVP + CRUD | patient-service |
| 2026-05-20 | Fix parse JWT clinicId | api-gateway |
| 2026-05-20 | Clinic Service MVP | clinic-service |
| 2026-05-20 | Auth register → clinic onboarding | auth-service, clinic-service |
| 2026-05-20 | Docs: progress/, domain boundaries, ADRs | docs |
| 2026-05-20 | FollowUp Service MVP + overdue scheduler | followup-service |

---

## Notas para próxima sesión

1. Abrir monorepo completo en Cursor (docs + backend + infra).
2. Progress log vive en `docs/progress/progress-log.md`.
3. Register requiere: `clinicName`, `country`, `timezone` para `CLINIC_ADMIN`.
4. `POST /api/clinics` público sigue siendo solo `PLATFORM_ADMIN`.
5. Siguiente milestone: Notification Service o tests de integración.
