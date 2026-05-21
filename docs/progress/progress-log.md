# CareFlow Progress Log

Documento vivo de **project memory** para sincronizar estado entre sesiones (Cursor, ChatGPT, GitHub).

> Actualizar después de cada implementación relevante.  
> Índice: [README.md](./README.md)

---

## Última actualización

**Fecha:** 2026-05-21  
**Sesión:** Staff invitation (DOCTOR/ASSISTANT) + sync documentación institucional  
**Branch activa:** `feat/staff-invitations` (pendiente merge a `main`)

---

## Estado general

CareFlow es una plataforma SaaS multi-tenant para consultorios y clínicas privadas. **Backend MVP operativo** con auth, clinic, patient, followup, hardening de errores/secrets, e **invitación multi-rol** (admin + doctores + asistentes en el mismo `clinicId`).

**Documentación institucional:**

| Doc | Ubicación |
|-----|-----------|
| Progress log | `docs/progress/progress-log.md` |
| ADRs | `docs/adr/` |
| Domain boundaries | `docs/architecture/domain-boundaries.md` |
| Service ownership | `docs/architecture/service-ownership.md` |
| Platform diagram | `docs/architecture/platform-diagram.md` |
| Data model | `docs/data-model/` |
| API specs + Postman | `docs/api/` |

---

## Infraestructura

| Componente | Estado | Notas |
|------------|--------|-------|
| Docker Compose | ✅ | PostgreSQL + RabbitMQ en `infra/docker/` |
| PostgreSQL | ✅ | `auth_db` (5433), `patient_db` (5434), `clinic_db` (5435), `followup_db` (5436) |
| RabbitMQ | ✅ | Preparado para notificaciones (fase futura) |
| Secrets | ✅ | `CAREFLOW_JWT_SECRET`, `CAREFLOW_INTERNAL_API_KEY` vía `.env.example` |

---

## Backend — Servicios implementados

### API Gateway (8080)

- JWT validation stateless
- Propagación `X-User-Id` / `X-Clinic-Id` / `X-Role`
- Rutas públicas auth: `login`, `register`, `register-clinic`, `register-invite`
- Ruta protegida auth: `POST /api/auth/invite` (requiere JWT)
- Proxy: auth, clinic, patient, followup
- Smoke tests WireMock

### Auth Service (8081)

| Feature | Detalle |
|---------|---------|
| Register CLINIC_ADMIN | Crea clínica vía `POST /internal/clinics` |
| Login | JWT con `userId`, `clinicId`, `role` |
| Errores | **401** credenciales inválidas, **409** email duplicado |
| Staff invite | `POST /auth/invite` — solo `CLINIC_ADMIN`, roles `DOCTOR`/`ASSISTANT` |
| Accept invite | `POST /auth/register-invite` — público, token + password |
| BD extra | Tabla `invitations` en `auth_db` |

### Clinic Service (8083)

CRUD clínicas (tenant raíz), ownership por rol, onboarding interno.

### Patient Service (8082)

CRUD pacientes scoped por `clinicId` del header.

### FollowUp Service (8084)

CRUD follow-ups, pending, complete/cancel, scheduler overdue → MISSED.

---

## Flujos principales

### 1. Onboarding consultorio nuevo (CLINIC_ADMIN)

```
POST /api/auth/register (CLINIC_ADMIN + clinicName/country/timezone)
  → auth-service → clinic-service POST /internal/clinics
  → user creado con clinicId real
  → login → JWT
  → pacientes / follow-ups operativos
```

### 2. Invitar doctor o asistente (multi-rol)

```
CLINIC_ADMIN login → JWT
POST /api/auth/invite { fullName, email, role: DOCTOR|ASSISTANT }
  → token de invitación (7 días, manual/WhatsApp por ahora)
Invitado → POST /api/auth/register-invite { token, password }
  → user en mismo clinicId
  → login → accede a datos de su consultorio
```

### 3. Request autenticado típico

```
Client → Gateway (JWT) → Service (X-Clinic-Id) → query scoped by clinicId
```

---

## Roles soportados

| Rol | Cómo se crea | Acceso MVP |
|-----|--------------|------------|
| `CLINIC_ADMIN` | Self-register | Full tenant (invite, CRUD clínico) |
| `DOCTOR` | Invitación | Mismo clinicId, sin invite |
| `ASSISTANT` | Invitación | Mismo clinicId, sin invite |
| `PLATFORM_ADMIN` | Manual/seed (futuro) | Gestión cross-tenant |

> Permisos diferenciados por rol en patient/followup **aún no implementados** — todos los roles autenticados acceden al CRUD clínico de su clínica.

---

## Issues resueltos

| Issue | Fix |
|-------|-----|
| 403 en `/api/auth/**` | `StripPrefix=1` en gateway |
| 500 JWT clinicId | Parser String → UUID en gateway |
| 500 login/register duplicado | Exception handler 401/409 |
| clinicId random en register | Integración auth → clinic-service |
| Solo 1 usuario por clínica | Staff invitation flow |

---

## Próximos pasos (prioridad sugerida)

1. [ ] **Merge PR** `feat/staff-invitations` → `main`
2. [ ] **Notification Service** — enviar invite token por email/WhatsApp (RabbitMQ, ADR 0003)
3. [ ] **Role-based access** — DOCTOR vs ASSISTANT en patient/followup (403 por acción)
4. [ ] **Frontend Next.js** — login, equipo, pacientes, follow-ups pending
5. [ ] **Externalizar DB credentials** (prod)
6. [ ] **ADR 0005** — staff invitation strategy (formalizar decisión)

---

## Historial de implementaciones

| Fecha | Implementación | Servicio |
|-------|----------------|----------|
| 2026-05-19 | JWT validation stateless | api-gateway |
| 2026-05-19 | StripPrefix auth routes | api-gateway |
| 2026-05-19 | Propagación headers identidad | api-gateway |
| 2026-05-20 | Patient Service MVP + CRUD | patient-service |
| 2026-05-20 | Clinic Service MVP | clinic-service |
| 2026-05-20 | Auth register → clinic onboarding | auth-service, clinic-service |
| 2026-05-20 | Docs: progress/, domain boundaries, ADRs | docs |
| 2026-05-20 | FollowUp Service MVP + overdue scheduler | followup-service |
| 2026-05-20 | Auth 409/401, externalized secrets, smoke tests | auth, gateway, docs |
| 2026-05-21 | Staff invitation DOCTOR/ASSISTANT | auth-service, api-gateway |

---

## Notas para próxima sesión

1. Progress log: `docs/progress/progress-log.md`
2. Register CLINIC_ADMIN requiere: `clinicName`, `country`, `timezone`
3. Invite requiere JWT de `CLINIC_ADMIN`; compartir `token` de respuesta manualmente
4. Reiniciar **gateway + auth-service** tras merge de invite (cambio rutas públicas/protegidas)
5. Postman smoke: `docs/api/careflow-smoke.postman_collection.json` (pasos 3–4 invite)
6. Siguiente milestone recomendado: **Notification Service** o **RBAC clínico**
