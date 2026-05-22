# CareFlow Progress Log

Documento vivo de **project memory** para sincronizar estado entre sesiones (Cursor, ChatGPT, GitHub).

> Actualizar después de cada implementación relevante.  
> Índice: [README.md](./README.md)

---

## Última actualización

**Fecha:** 2026-05-19  
**Sesión:** Fase 0 — commit frontend MVP + docs demo/onboarding + contrato eventos notificaciones

---

## Estado general

CareFlow es una plataforma SaaS multi-tenant para consultorios y clínicas privadas. **Producto usable en local:** backend MVP + web app con login, dashboard, pacientes, seguimientos, equipo e invitaciones.

**Demo local:** [docs/demo/local-demo.md](../demo/local-demo.md)

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
| PostgreSQL | ✅ | `auth_db` (5433), `patient_db` (5434), `clinic_db` (5435), `followup_db` (5436), `notification_db` (5437) |
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

### Notification Service (8085)

| Feature | Detalle |
|---------|---------|
| RabbitMQ consumer | `followup.scheduled`, `followup.missed` |
| Canal demo | `WHATSAPP_LINK` (wa.me) |
| REST | `GET /notifications`, `POST /notifications/{id}/send` |
| BD | `notification_db` :5437 |

### Frontend Web App (3000)

Next.js MVP: login, dashboard, pacientes, seguimientos, equipo (invite). Proxy `/api` → gateway `:8080`.

| Aspecto | Estado |
|---------|--------|
| Pantallas MVP | ✅ |
| UI español LATAM (`labels.ts`) | ✅ roles, estados, tipos seguimiento |
| AuthGuard hidratación | ✅ evita mismatch SSR/localStorage |
| Registro consultorio (`/register`) | ✅ |
| Deploy staging | ❌ |
| Commit en `main` | ✅ |

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

### Para demo presentable

1. [x] **Commit frontend** — versionar `frontend/` en `main`
2. [x] **Notification Service (Fase 1)** — ver [notification-events.md](../api/notification-events.md)
3. [ ] **Deploy staging** — URL pública (VPS Linux + Docker)

### Mejoras posteriores

5. [ ] **RBAC clínico** — permisos DOCTOR vs ASSISTANT en UI/API
6. [ ] **Polish UI** — toasts, skeletons, script arranque local
7. [ ] Externalizar DB credentials (prod)

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
| 2026-05-21 | Frontend Next.js MVP | frontend |
| 2026-05-19 | Frontend i18n español + AuthGuard hydration fix | frontend |
| 2026-05-19 | Fase 0: frontend commit + docs demo + eventos notificaciones | frontend, docs |

---

## Notas para próxima sesión

1. Progress log: `docs/progress/progress-log.md`
2. Demo local: `docs/demo/local-demo.md`
3. Register CLINIC_ADMIN: pantalla `/register` (self-service)
4. Invite requiere JWT de `CLINIC_ADMIN`; compartir `token` manualmente hasta Notification Service
5. Frontend: `cd frontend && npm run dev` → `:3000`
6. Siguiente milestone: **Notification Service Fase 1** — [notification-events.md](../api/notification-events.md)
