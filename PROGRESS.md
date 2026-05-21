# CareFlow Platform — Progress Log

Documento vivo para sincronizar el estado del proyecto entre sesiones (Cursor, ChatGPT, GitHub).

> Actualizar este archivo después de cada implementación relevante.

---

## Última actualización

**Fecha:** 2026-05-20  
**Sesión:** Clinic Service MVP — tenants de nivel superior

---

## Estado general

CareFlow es una plataforma SaaS multi-tenant para clínicas de salud. El proyecto ya supera la fase de tutorial: tiene infraestructura real, arquitectura documentada, auth distribuida end-to-end y base multi-tenant.

---

## Infraestructura

| Componente | Estado | Notas |
|------------|--------|-------|
| Docker Compose | ✅ | PostgreSQL + RabbitMQ en `infra/docker/` |
| PostgreSQL | ✅ | `auth_db` (5433), `patient_db` (5434), `clinic_db` (5435) |
| RabbitMQ | ✅ | Preparado para notificaciones (fase futura) |

---

## Arquitectura y documentación

| Elemento | Estado | Ubicación |
|----------|--------|-----------|
| Monorepo | ✅ | Raíz del workspace |
| ADRs | ✅ | `docs/adr/` |
| Spec-driven | ✅ | `docs/api/`, `docs/data-model/` |
| Multi-tenant model | ✅ | `docs/architecture/multitenancy.md` |
| Backend roadmap | ✅ | `docs/architecture/backend-roadmap.md` |

### Modelo multi-tenant

- **Tenant = Clinic** (`clinicId`)
- JWT claims obligatorios: `userId`, `clinicId`, `role`
- Roles: `PLATFORM_ADMIN`, `CLINIC_ADMIN`, `DOCTOR`, `ASSISTANT`
- Aislamiento por `clinicId` en capa de aplicación

---

## Backend — Servicios implementados

### API Gateway (puerto 8080)

| Feature | Estado | Detalle |
|---------|--------|---------|
| Spring Cloud Gateway WebFlux | ✅ | |
| Health endpoint | ✅ | `GET /health` |
| Proxy auth-service | ✅ | `/api/auth/**` → `localhost:8081` |
| StripPrefix | ✅ | Quita `/api` → reenvía `/auth/**` al auth-service |
| JWT validation (jjwt 0.12.5) | ✅ | Stateless, Spring Security WebFlux |
| Rutas públicas | ✅ | `/health/**`, `/api/auth/**` |
| Propagación identidad | ✅ | Headers `X-User-Id`, `X-Clinic-Id`, `X-Role` |
| Proxy patient-service | ✅ | `/api/patients/**` → `localhost:8082` |
| Proxy clinic-service | ✅ | `/api/clinics/**` → `localhost:8083` |

**Clases clave (gateway):**

- `service/JwtService.java` — valida JWT y extrae claims
- `security/JwtAuthenticationFilter.java` — filtro Bearer token
- `security/JwtClaims.java` — record con userId, clinicId, role
- `security/JwtAuthenticationToken.java` — token autenticado
- `security/TenantIdentityHeaders.java` — constantes de headers
- `security/SecurityConfig.java` — config stateless + rutas públicas
- `filter/TenantIdentityPropagationFilter.java` — inyecta headers downstream

**Config JWT:**

```yaml
careflow:
  jwt:
    secret: careflow-secret-key-careflow-secret-key  # usar env var en prod
```

### Auth Service (puerto 8081)

| Feature | Estado | Detalle |
|---------|--------|---------|
| Register | ✅ | `POST /auth/register` |
| Login | ✅ | `POST /auth/login` → devuelve JWT |
| BCrypt | ✅ | Hash de contraseñas |
| JWT generation (jjwt 0.12.5) | ✅ | Claims: sub=userId, clinicId, role |
| PostgreSQL / JPA | ✅ | Entidad `User` |
| Rutas públicas | ✅ | `/auth/**`, `/actuator/**` |

**Pendiente menor:** manejo de email duplicado en register (hoy devuelve 500, debería ser 409 Conflict).

**Integración register → clinic-service:** ✅ Al registrar `CLINIC_ADMIN`, auth-service crea clínica vía `POST /internal/clinics` y usa el `clinicId` real.

### Patient Service (puerto 8082)

| Feature | Estado | Detalle |
|---------|--------|---------|
| CRUD MVP | ✅ | POST/GET/PUT/DELETE /patients |
| PostgreSQL | ✅ | `patient_db` en puerto 5434 |
| TenantContext | ✅ | Lee headers X-User-Id, X-Clinic-Id, X-Role |
| Aislamiento clinicId | ✅ | Queries siempre filtradas por clinicId del contexto |
| Exception handling | ✅ | 401, 404, 400 vía ProblemDetail |

**Clases clave (patient-service):**

- `entity/Patient.java` — aggregate con clinicId obligatorio
- `entity/PatientStatus.java` — ACTIVE, AT_RISK, INACTIVE
- `dto/CreatePatientRequest.java`, `dto/UpdatePatientRequest.java`, `dto/PatientResponse.java`
- `repository/PatientRepository.java` — findByClinicId, findByIdAndClinicId
- `service/PatientService.java` — lógica de negocio con tenant isolation
- `controller/PatientController.java` — REST endpoints
- `tenant/TenantContext.java` — identidad request-scoped (ThreadLocal)
- `tenant/TenantContextFilter.java` — extrae headers del gateway
- `tenant/TenantHeaders.java` — constantes de headers
- `exception/GlobalExceptionHandler.java` — manejo centralizado de errores
- `security/SecurityConfig.java` — auth delegada al gateway

### Clinic Service (puerto 8083)

| Feature | Estado | Detalle |
|---------|--------|---------|
| CRUD MVP | ✅ | POST/GET/PUT/DELETE /clinics |
| PostgreSQL | ✅ | `clinic_db` en puerto 5435 |
| Top-level tenant | ✅ | Clinic = tenant raíz del SaaS |
| Role-based access | ✅ | PLATFORM_ADMIN vs CLINIC_ADMIN |
| Soft delete | ✅ | DELETE marca `active=false` |
| Exception handling | ✅ | 401, 403, 404, 400 |

**Clases clave (clinic-service):**

- `entity/Clinic.java` — tenant raíz (id, name, country, timezone, subscriptionPlan, active)
- `entity/SubscriptionPlan.java` — FREE, BASIC, PRO
- `dto/CreateClinicRequest.java`, `dto/UpdateClinicRequest.java`, `dto/ClinicResponse.java`
- `repository/ClinicRepository.java`
- `service/ClinicService.java` — reglas de ownership por rol
- `controller/ClinicController.java`
- `security/ClinicAccessGuard.java` — PLATFORM_ADMIN vs own clinic
- `tenant/TenantContext.java`, `TenantContextFilter.java`
- `exception/GlobalExceptionHandler.java`

**Modelo de ownership:**

| Rol | POST | GET list | GET/{id} | PUT | DELETE |
|-----|------|----------|----------|-----|--------|
| PLATFORM_ADMIN | ✅ todas | ✅ activas | ✅ cualquiera | ✅ cualquiera | ✅ soft delete |
| CLINIC_ADMIN | ❌ 403 | ✅ solo la suya | ✅ solo la suya | ✅ solo la suya | ❌ 403 |
| DOCTOR/ASSISTANT | ❌ 403 | ✅ solo la suya | ✅ solo la suya | ❌ 403 | ❌ 403 |

---

## Flujo de autenticación end-to-end

```
Cliente
  │  POST /api/auth/login
  ▼
API Gateway (8080)
  │  StripPrefix: /api/auth/login → /auth/login
  ▼
Auth Service (8081)
  │  Valida credenciales, genera JWT
  ▼
Cliente recibe token

---

Cliente
  │  GET /api/... + Authorization: Bearer <JWT>
  ▼
API Gateway
  │  1. JwtAuthenticationFilter valida token
  │  2. TenantIdentityPropagationFilter inyecta:
  │     X-User-Id, X-Clinic-Id, X-Role
  ▼
Downstream Service (Patient Service)
  │  Lee X-Clinic-Id para filtrar datos
  ▼
Respuesta
```

---

## Issues resueltos en sesiones anteriores

### 403 Forbidden en `/api/auth/**`

**Causa:** Gateway reenviaba `/api/auth/login` sin transformar; auth-service espera `/auth/login`.  
**Fix:** `StripPrefix=1` en la ruta del gateway.

### Register falla con email existente

**Causa:** Usuario ya registrado directamente contra auth-service (`abel@test.com`).  
**Comportamiento:** Constraint único en PostgreSQL → 500. Login funciona correctamente.

### 500 en `/api/patients` — JWT clinicId parse error

**Causa:** Gateway intentaba leer `clinicId` como `UUID.class`; JJWT lo devuelve como `String`.  
**Fix:** Parser flexible con `getClaimAsString()` + `UUID.fromString()`. Auth-service emite `clinicId.toString()`.

### Pacientes no visibles entre sesiones de login

**Causa:** `auth-service` genera `clinicId` con `UUID.randomUUID()` en cada registro. Usuarios distintos o re-registros tienen clínicas distintas.  
**Comportamiento esperado:** Aislamiento multi-tenant funciona. **Clinic Service** ya existe; pendiente integrar auth register con POST /clinics.

---

## Próximos pasos (roadmap)

### Inmediato

- [x] Clinic Service MVP
- [x] Integrar auth register → crear clinic real + asignar clinicId
- [ ] Tests de integración gateway → patient-service

### Corto plazo

- [ ] Exception handler en auth-service (409 email duplicado, 401 credenciales inválidas)
- [ ] Externalizar JWT secret vía env var en todos los servicios
- [ ] FollowUp Service

### Fase 2+

- FollowUp Service
- Notification Service (RabbitMQ)
- WhatsApp integration
- AI risk scoring

---

## Cómo levantar el entorno local

```bash
# Infra
cd infra/docker
docker compose up -d

# Auth Service
cd backend/auth-service
mvn spring-boot:run

# API Gateway
cd backend/api-gateway
mvn spring-boot:run

# Patient Service
cd backend/patient-service
mvn spring-boot:run

# Clinic Service
cd backend/clinic-service
mvn spring-boot:run
```

### Pruebas manuales sugeridas

```bash
# Health (público)
curl http://localhost:8080/health

# Login (público) — guarda el token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jorge@test.com","password":"123456"}'

# Crear paciente (protegido)
curl -X POST http://localhost:8080/api/patients \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Juan Pérez","phoneNumber":"+51999111222","diagnosis":"Hipertensión"}'

# Listar pacientes de la clínica (protegido)
curl http://localhost:8080/api/patients \
  -H "Authorization: Bearer <token>"

# Obtener paciente por ID (protegido)
curl http://localhost:8080/api/patients/<patient-id> \
  -H "Authorization: Bearer <token>"

# Actualizar paciente (protegido)
curl -X PUT http://localhost:8080/api/patients/<patient-id> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Juan Pérez","phoneNumber":"+51999111222","diagnosis":"Hipertensión controlada","status":"AT_RISK"}'

# Eliminar paciente (protegido)
curl -X DELETE http://localhost:8080/api/patients/<patient-id> \
  -H "Authorization: Bearer <token>"

# Crear clínica (PLATFORM_ADMIN)
curl -X POST http://localhost:8080/api/clinics \
  -H "Authorization: Bearer <platform-admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Clínica Lima Norte","country":"PE","timezone":"America/Lima","subscriptionPlan":"BASIC"}'

# Ver clínica propia (CLINIC_ADMIN)
curl http://localhost:8080/api/clinics/<clinic-id> \
  -H "Authorization: Bearer <token>"
```

---

## Historial de implementaciones

| Fecha | Implementación | Servicio |
|-------|----------------|----------|
| 2026-05-19 | JWT validation stateless en gateway | api-gateway |
| 2026-05-19 | Fix StripPrefix para rutas auth | api-gateway |
| 2026-05-19 | Propagación headers X-User-Id, X-Clinic-Id, X-Role | api-gateway |
| 2026-05-20 | Patient Service MVP con tenant isolation | patient-service |
| 2026-05-20 | Fix parse JWT clinicId (String → UUID) | api-gateway |
| 2026-05-20 | Clinic Service MVP con ownership por rol | clinic-service |
| 2026-05-20 | Integración auth register → clinic-service onboarding | auth-service, clinic-service |

---

## Notas para ChatGPT / próxima sesión

1. El monorepo completo debe abrirse en Cursor (docs + backend + infra).
2. Auth distribuida funciona end-to-end: login via gateway devuelve JWT válido.
3. Gateway ya propaga identidad multi-tenant a servicios downstream.
4. Patient Service CRUD completo verificado: POST, GET list, GET by id, PUT, DELETE.
5. Clinic Service MVP: tenants de nivel superior con CRUD y reglas por rol.
6. Siguiente milestone: integrar auth register con Clinic Service, FollowUp Service.
6. Register CLINIC_ADMIN crea clínica real en clinic-service automáticamente.
7. Siguiente milestone: FollowUp Service, invitación DOCTOR/ASSISTANT a clínica existente.
