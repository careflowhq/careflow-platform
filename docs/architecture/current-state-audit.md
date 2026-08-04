# Auditoría técnica — CareFlow Platform (estado actual)

**Fecha de auditoría:** 2026-08-03  
**Alcance:** Repositorio completo `careflow-platform` (backend, frontend, infra, docs)  
**Propósito:** Baseline previo a modernización de arquitectura  
**Metodología:** Análisis estático del código, configuración y documentación existente. Sin modificaciones al código.

---

# 1. Resumen general

## Nombre del proyecto

**CareFlow Platform** — plataforma SaaS multi-tenant para consultorios y clínicas privadas, orientada a seguimiento de pacientes, retención y gestión de cuidados crónicos.

## Tecnologías utilizadas

| Capa | Stack |
|------|-------|
| Backend | Spring Boot 3.5.14, Java 21, Spring Cloud Gateway 2025.0.2 |
| Frontend | Next.js 16.2.6, React 19, TypeScript 5 |
| Base de datos | PostgreSQL 16 (database-per-service) |
| Mensajería | RabbitMQ 3 (topic exchange) |
| Contenedores | Docker, Docker Compose |
| Proxy / TLS | Nginx 1.27, Let's Encrypt (Certbot) |
| Build | Maven 3.9, Node 20 |

## Lenguajes

- **Java 21** — 6 microservicios backend (~136 clases)
- **TypeScript / TSX** — frontend (~33 archivos fuente)
- **Bash / PowerShell** — scripts de arranque, deploy y SSL
- **YAML / Markdown** — configuración y documentación

## Frameworks

- Spring Boot (Web MVC, WebFlux/Gateway, Security, Data JPA, AMQP, Actuator, Validation)
- Next.js App Router
- TanStack React Query, Zustand, React Hook Form + Zod
- Tailwind CSS 4

## Arquitectura general

Arquitectura de **microservicios** con:

- **API Gateway** como único punto de entrada HTTP autenticado (JWT)
- **Database-per-service** (5 bases PostgreSQL lógicamente separadas)
- **Multi-tenancy** por `clinicId` propagado vía headers HTTP (`X-Clinic-Id`, `X-User-Id`, `X-Role`)
- **Comunicación síncrona** HTTP (RestClient) entre auth→clinic y followup→patient
- **Comunicación asíncrona** RabbitMQ (followup→notification)
- **Frontend SPA/SSR híbrido** (Next.js) con proxy `/api` al gateway
- **Deploy staging** en VPS Hetzner con Docker Compose + Nginx + HTTPS

```
                    ┌─────────────┐
  Browser ─────────►│   Nginx     │ (staging: 80/443)
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
       ┌─────────────┐           ┌─────────────┐
       │  Frontend   │           │ API Gateway │ :8080
       │  Next.js    │           │  (WebFlux)  │
       └─────────────┘           └──────┬──────┘
                                        │
         ┌──────────┬──────────┬───────┼───────┬──────────┐
         ▼          ▼          ▼       ▼       ▼          ▼
      auth:8081  patient:8082 clinic:8083 followup:8084 notif:8085
         │                      ▲          │          │
         └──── HTTP internal ───┘          │          │
                                           │    RabbitMQ
                                           └──────────► notification
```

## Estado general del proyecto

| Dimensión | Evaluación |
|-----------|------------|
| **Madurez funcional** | MVP operativo end-to-end (registro, login, pacientes, seguimientos, notificaciones demo, invitaciones staff) |
| **Entorno local** | Funcional con scripts `start-local.*` + Docker infra |
| **Staging** | Desplegado en VPS Hetzner (`178.105.118.30`), HTTPS activo (`app.careflowhq.org`, `careflowhq.org`) |
| **Tests automatizados** | Limitados — integración en auth-service y api-gateway; smoke tests en resto |
| **Migraciones BD** | Flyway v1 baseline por servicio; `ddl-auto: validate` |
| **Observabilidad** | Actuator health/info únicamente |
| **Documentación** | Extensa en `docs/` (ADRs, data model, deploy, API, progress log) |
| **Deuda técnica principal** | Duplicación multi-tenant, `shared-libs` vacío, RBAC parcial, schema sin versionar |

---

# 2. Estructura del repositorio

## Árbol de carpetas importantes

```
careflow-platform/
├── .env.example                    # Secrets locales (JWT, internal API key)
├── README.md
├── PROGRESS.md
│
├── backend/
│   ├── api-gateway/                # Spring Cloud Gateway, JWT, routing
│   ├── auth-service/               # Identidad, login, registro, invitaciones
│   ├── patient-service/            # CRUD pacientes
│   ├── clinic-service/             # CRUD clínicas (tenant raíz)
│   ├── followup-service/           # Seguimientos + scheduler + RabbitMQ producer
│   ├── notification-service/       # RabbitMQ consumer + notificaciones WhatsApp demo
│   └── shared-libs/                # (vacío — placeholder ADR 0004)
│
├── frontend/
│   ├── src/
│   │   ├── app/                    # App Router (login, register, dashboard, CRUD pages)
│   │   ├── components/             # layout (AuthGuard, AppShell) + ui
│   │   ├── lib/                    # api client, auth-store, jwt, utils
│   │   ├── providers/              # React Query
│   │   └── types/
│   ├── Dockerfile
│   └── package.json
│
├── infra/
│   └── docker/
│       ├── docker-compose.yml              # Dev: 5 Postgres + RabbitMQ
│       ├── docker-compose.staging.yml      # Staging: stack completo
│       ├── Dockerfile.spring-service       # Build genérico Java
│       ├── .env.staging.example
│       ├── nginx/                          # default.conf, default.ssl.conf
│       ├── certbot/                        # certs Let's Encrypt (gitignored conf/)
│       └── postgres/init-databases.sh      # Crea 5 DB en staging
│
├── docs/
│   ├── adr/                        # Architecture Decision Records (4 ADRs)
│   ├── api/                        # OpenAPI, Postman, eventos RabbitMQ
│   ├── architecture/               # Diagramas, boundaries, ownership
│   ├── business-rules/
│   ├── data-model/                 # Modelo documentado por entidad
│   ├── demo/
│   ├── deploy/                     # Local, Hetzner, staging, SSL
│   ├── product/
│   ├── progress/
│   └── workflows/
│
└── scripts/
    ├── start-local.ps1 / .sh       # Arranque dev completo
    ├── stop-local.ps1 / .sh
    ├── deploy-staging.sh
    ├── setup-ssl.sh
    ├── renew-ssl.sh
    └── install-ssl-renew-cron.sh
```

## Propósito de cada módulo

| Módulo | Propósito |
|--------|-----------|
| `backend/api-gateway` | Enrutamiento, validación JWT, propagación de identidad tenant |
| `backend/auth-service` | Autenticación, registro CLINIC_ADMIN, invitaciones staff, emisión JWT |
| `backend/clinic-service` | Agregado Clinic (tenant), CRUD y endpoint interno de onboarding |
| `backend/patient-service` | Agregado Patient scoped por clínica |
| `backend/followup-service` | Agregado FollowUp, scheduler overdue, publicación eventos |
| `backend/notification-service` | Consumo eventos, generación links WhatsApp demo, historial |
| `backend/shared-libs` | Reservado para librerías compartidas (sin implementar) |
| `frontend` | UI web en español (LATAM), proxy API, auth client-side |
| `infra/docker` | Definición de contenedores dev y staging |
| `docs/` | Documentación institucional, ADRs, guías operativas |
| `scripts/` | Automatización local y deploy VPS |

---

# 3. Microservicios

## Resumen

| Servicio | Puerto | Base de datos | ORM |
|----------|--------|---------------|-----|
| api-gateway | 8080 | — | — |
| auth-service | 8081 | `auth_db` | Spring Data JPA / Hibernate |
| patient-service | 8082 | `patient_db` | Spring Data JPA / Hibernate |
| clinic-service | 8083 | `clinic_db` | Spring Data JPA / Hibernate |
| followup-service | 8084 | `followup_db` | Spring Data JPA / Hibernate |
| notification-service | 8085 | `notification_db` | Spring Data JPA / Hibernate |

---

## 3.1 api-gateway

**Responsabilidades:**
- Punto de entrada único HTTP para el frontend
- Validación JWT stateless (HS256)
- Eliminación de headers tenant spoofeados e inyección de headers confiables
- Enrutamiento con `StripPrefix=1` a microservicios

**Endpoints principales:**

| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/health` | Health check (público) |
| * | `/api/auth/**` | Proxy → auth-service |
| * | `/api/patients/**` | Proxy → patient-service |
| * | `/api/clinics/**` | Proxy → clinic-service |
| * | `/api/followups/**` | Proxy → followup-service |
| * | `/api/notifications/**` | Proxy → notification-service |

**Dependencias:**
- Todos los microservicios backend (proxy HTTP)
- Secret compartido `CAREFLOW_JWT_SECRET` con auth-service

**Stack:** Spring Cloud Gateway (WebFlux), Spring Security WebFlux, JJWT 0.12.5

---

## 3.2 auth-service

**Responsabilidades:**
- Registro de `CLINIC_ADMIN` con onboarding de clínica
- Login y emisión JWT (24h, claims: sub, clinicId, role)
- Invitación de staff (`DOCTOR`, `ASSISTANT`)
- Aceptación de invitación (`register-invite`)

**Endpoints principales:**

| Método | Path (servicio) | Path (gateway) | Auth |
|--------|-----------------|----------------|------|
| POST | `/auth/register` | `/api/auth/register` | Público |
| POST | `/auth/register-clinic` | `/api/auth/register-clinic` | Público (alias) |
| POST | `/auth/register-invite` | `/api/auth/register-invite` | Público |
| POST | `/auth/login` | `/api/auth/login` | Público |
| POST | `/auth/invite` | `/api/auth/invite` | JWT + CLINIC_ADMIN |

**Dependencias:**
- **→ clinic-service:** `POST /internal/clinics` vía `ClinicServiceClient` (header `X-Internal-Api-Key`)

**Entidades JPA:** `User`, `Invitation`

---

## 3.3 clinic-service

**Responsabilidades:**
- CRUD del agregado Clinic (tenant raíz)
- Control de acceso por rol (`ClinicAccessGuard`)
- Endpoint interno para creación de clínica durante registro

**Endpoints principales:**

| Método | Path | Auth |
|--------|------|------|
| POST | `/clinics` | Headers tenant |
| GET | `/clinics` | Headers tenant |
| GET | `/clinics/{id}` | Headers tenant |
| PUT | `/clinics/{id}` | Headers tenant |
| DELETE | `/clinics/{id}` | Headers tenant |
| POST | `/internal/clinics` | `X-Internal-Api-Key` (no expuesto en gateway) |

**Dependencias:** Ninguna HTTP saliente

**Entidades JPA:** `Clinic`

---

## 3.4 patient-service

**Responsabilidades:**
- CRUD de pacientes aislados por `clinicId`
- `clinicId` siempre del header gateway, nunca del body del cliente

**Endpoints principales:**

| Método | Path (gateway) |
|--------|----------------|
| POST | `/api/patients` |
| GET | `/api/patients` |
| GET | `/api/patients/{id}` |
| PUT | `/api/patients/{id}` |
| DELETE | `/api/patients/{id}` |

**Dependencias:**
- **← followup-service:** consultado vía `GET /patients/{id}` (RestClient)

**Entidades JPA:** `Patient`

---

## 3.5 followup-service

**Responsabilidades:**
- CRUD de seguimientos clínicos
- Listado de pendientes
- Completar / cancelar seguimientos
- Scheduler: marca `PENDING` vencidos como `MISSED` (cron cada 15 min)
- Publicación eventos RabbitMQ

**Endpoints principales:**

| Método | Path (gateway) |
|--------|----------------|
| POST | `/api/followups` |
| GET | `/api/followups` |
| GET | `/api/followups/pending` |
| GET | `/api/followups/{id}` |
| PUT | `/api/followups/{id}` |
| PATCH | `/api/followups/{id}/complete` |
| DELETE | `/api/followups/{id}` (cancelar) |

**Dependencias:**
- **→ patient-service:** `GET /patients/{id}` (`PatientServiceClient`)
- **→ RabbitMQ:** exchange `careflow.events` (`FollowUpEventPublisher`)

**Entidades JPA:** `FollowUp`

---

## 3.6 notification-service

**Responsabilidades:**
- Consumir eventos `followup.scheduled` y `followup.missed`
- Crear registros de notificación con mensaje WhatsApp demo (`wa.me`)
- Idempotencia por `eventId`
- API REST para listar y marcar como enviada

**Endpoints principales:**

| Método | Path (gateway) |
|--------|----------------|
| GET | `/api/notifications` |
| POST | `/api/notifications/{id}/send` |

**Dependencias:**
- **← RabbitMQ** desde followup-service
- Config `careflow.patient-service.base-url` presente pero **sin cliente HTTP implementado**

**Entidades JPA:** `Notification`

---

# 4. Frontend

## Framework

**Next.js 16.2.6** con App Router, React 19.2.4, TypeScript 5.

## Librerías principales

| Librería | Uso |
|----------|-----|
| `axios` | Cliente HTTP con interceptores JWT |
| `zustand` + persist | Estado de autenticación (token en localStorage) |
| `jwt-decode` | Lectura de claims/exp en cliente (sin verificación criptográfica) |
| `@tanstack/react-query` | Cache y mutations de datos API |
| `react-hook-form` + `zod` | Formularios y validación |
| `tailwindcss` 4 | Estilos |
| `lucide-react` | Iconografía |

## Estructura

```
frontend/src/
├── app/
│   ├── page.tsx                    # Landing
│   ├── login/, register/, register-invite/
│   └── (app)/                      # Rutas protegidas
│       ├── layout.tsx              # AuthGuard + AppShell
│       ├── dashboard/
│       ├── patients/
│       ├── followups/
│       ├── notifications/
│       └── team/
├── components/layout/              # auth-guard, app-shell
├── components/ui/                  # button, card, input, badge...
├── lib/api/                        # client, auth, patients, followups, notifications
├── lib/auth-store.ts
├── lib/jwt.ts
└── types/index.ts
```

## Comunicación con backend

1. **Rewrite Next.js:** `/api/*` → `${API_GATEWAY_URL}/api/*` (`next.config.ts`)
2. **Axios** con `baseURL: "/api"` (`lib/api/client.ts`)
3. **Interceptor request:** adjunta `Authorization: Bearer <token>`
4. **Interceptor response:** en 401 → logout + redirect `/login`
5. **React Query** en páginas CRUD con `staleTime: 30s`

En staging, Nginx enruta `/api/` directamente al gateway; el frontend usa rewrite interno en build.

## Manejo de autenticación

| Aspecto | Implementación |
|---------|----------------|
| Almacenamiento | Zustand persist → `localStorage` clave `careflow-auth` |
| Protección rutas | **Client-side** `AuthGuard` en layout `(app)` — **no hay middleware Next.js** |
| Login | `POST /api/auth/login` → guarda token → redirect `/dashboard` |
| Registro | `/register` → `POST /api/auth/register` → auto-login |
| Invitación | `/register-invite?token=...` → `POST /api/auth/register-invite` |
| RBAC UI | Oculta nav "Equipo" si `role !== CLINIC_ADMIN` (cosmético) |
| Expiración | 24h — re-login manual, sin refresh token |

---

# 5. Bases de datos

## Inventario

| Base de datos | Servicio | Puerto local (host) | Staging |
|---------------|----------|---------------------|---------|
| `auth_db` | auth-service | 5433 | postgres:5432/auth_db |
| `patient_db` | patient-service | 5434 | postgres:5432/patient_db |
| `clinic_db` | clinic-service | 5435 | postgres:5432/clinic_db |
| `followup_db` | followup-service | 5436 | postgres:5432/followup_db |
| `notification_db` | notification-service | 5437 | postgres:5432/notification_db |

**ORM:** Spring Data JPA con Hibernate en todos los servicios con persistencia.

**Migraciones:** **Flyway** (Sprint 2). Cada servicio JPA tiene `db/migration/V1__baseline.sql`. Configuración:

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.baseline-on-migrate: true
spring.flyway.baseline-version: 1
```

Ver `docs/architecture/database-migrations.md`.

## Tablas principales y relaciones

> No hay foreign keys cross-database. Las relaciones son **lógicas** vía UUID almacenados en cada servicio.

### auth_db

| Tabla | Campos clave | Relaciones lógicas |
|-------|--------------|-------------------|
| `users` | id, clinicId, email, passwordHash, role, fullName | clinicId → clinics.id (clinic_db) |
| `invitations` | id, token, clinicId, email, role, status, expiresAt, invitedBy | clinicId → clinics; invitedBy → users |

### clinic_db

| Tabla | Campos clave | Relaciones lógicas |
|-------|--------------|-------------------|
| `clinics` | id, name, country, timezone, subscriptionPlan, active | Raíz del tenant |

### patient_db

| Tabla | Campos clave | Relaciones lógicas |
|-------|--------------|-------------------|
| `patients` | id, clinicId, fullName, phoneNumber, diagnosis, status, assignedDoctorId, createdAt | clinicId → clinics; assignedDoctorId → users |

### followup_db

| Tabla | Campos clave | Relaciones lógicas |
|-------|--------------|-------------------|
| `followups` | id, clinicId, patientId, doctorId, type, scheduledDate, status, notes, createdBy, createdAt | patientId → patients; clinicId → clinics |

Índices: `(clinicId, status)`, `(clinicId, scheduledDate)`

### notification_db

| Tabla | Campos clave | Relaciones lógicas |
|-------|--------------|-------------------|
| `notifications` | id, clinicId, patientId, followUpId, eventId (unique), eventType, channel, status, message, deliveryUrl, sentAt | followUpId → followups; eventId idempotencia |

Índices: `(clinicId, createdAt)`, unique `(eventId)`

## Diagrama lógico cross-service

```
clinics (clinic_db)
    │
    ├── users (auth_db)           clinicId
    ├── invitations (auth_db)     clinicId
    ├── patients (patient_db)     clinicId
    ├── followups (followup_db)   clinicId + patientId
    └── notifications (notification_db)  clinicId + followUpId + eventId
```

---

# 6. Seguridad

## Autenticación

- **Modelo:** JWT Bearer stateless (HS256)
- **Emisión:** auth-service (`JwtService`) — expiración 24h
- **Validación:** api-gateway (`JwtAuthenticationFilter` + `JwtService`)
- **Claims:** `sub` (userId), `clinicId`, `role`
- **Passwords:** BCrypt en auth-service

## Autorización

- **Gateway:** rutas públicas limitadas a auth endpoints; resto `authenticated()`
- **Microservicios:** Spring Security con `permitAll()` — **no validan JWT directamente**
- **Tenant isolation:** `TenantContextFilter` exige headers `X-User-Id`, `X-Clinic-Id`, `X-Role` → 401 si faltan
- **RBAC aplicación:**
  - `InviteAccessGuard` — solo `CLINIC_ADMIN` puede invitar
  - `ClinicAccessGuard` — `PLATFORM_ADMIN` vs `CLINIC_ADMIN` en clinic-service
  - **patient/followup/notification:** solo aislamiento por clinicId, sin diferenciación DOCTOR/ASSISTANT

## JWT

| Aspecto | Detalle |
|---------|---------|
| Algoritmo | HS256 |
| Librería | JJWT 0.12.5 |
| Secret | `CAREFLOW_JWT_SECRET` (env var, default dev en YAML) |
| Propagación | Gateway elimina headers tenant del cliente e inyecta desde JWT |

## Filtros

| Filtro | Ubicación | Función |
|--------|-----------|---------|
| `JwtAuthenticationFilter` | api-gateway | Valida Bearer token |
| `TenantIdentityPropagationFilter` | api-gateway | Anti-spoofing + inyección headers |
| `TenantContextFilter` | 5 servicios | Parsea headers → `TenantContext` thread-local |
| `InternalApiKeyFilter` | clinic-service | Protege `/internal/**` |

## Spring Security

- **Gateway:** WebFlux Security, CSRF off, stateless, NoOp security context
- **Servicios:** Servlet Security, `permitAll()` + filtros custom tenant

## Roles

| Rol | Enum | Creación | Permisos actuales |
|-----|------|----------|-------------------|
| `PLATFORM_ADMIN` | UserRole | Manual/futuro | Acceso cross-tenant en clinic-service |
| `CLINIC_ADMIN` | UserRole | Self-register | Invite staff, CRUD completo tenant |
| `DOCTOR` | UserRole | Invitación | CRUD clínico tenant (sin invite) |
| `ASSISTANT` | UserRole | Invitación | CRUD clínico tenant (sin invite) |

---

# 7. Mensajería

## RabbitMQ

**Infra:** contenedor `rabbitmq:3-management` (local puertos 5672/15672; staging solo red interna).

**Credenciales default:** guest/guest (local y staging).

## Exchange

| Nombre | Tipo | Durable | Definido en |
|--------|------|---------|-------------|
| `careflow.events` | topic | true | `RabbitConfig` (followup + notification) |

## Queues

| Nombre | Durable | Consumer |
|--------|---------|----------|
| `notification-service.queue` | true | notification-service |

## Bindings

| Routing key | Queue |
|-------------|-------|
| `followup.scheduled` | `notification-service.queue` |
| `followup.missed` | `notification-service.queue` |

## Producers

| Servicio | Clase | Eventos |
|----------|-------|---------|
| followup-service | `FollowUpEventPublisher` | `followup.scheduled` (al crear), `followup.missed` (scheduler) |

## Consumers

| Servicio | Clase | Handler |
|----------|-------|---------|
| notification-service | `NotificationEventListener` | `@RabbitListener` → `NotificationService.handleEvent()` |

## Eventos publicados

**Payload:** record `CareFlowEvent` (duplicado en followup y notification services)

```java
record CareFlowEvent(
    String eventType,      // routing key
    UUID eventId,          // idempotencia
    Instant occurredAt,
    UUID clinicId,
    UUID patientId,
    UUID followUpId,
    Map<String, Object> payload  // type, scheduledDate, patientName, patientPhone
)
```

| eventType | Disparador | Efecto en notification-service |
|-----------|------------|-------------------------------|
| `followup.scheduled` | `FollowUpService.create()` | Crea Notification WHATSAPP_LINK READY |
| `followup.missed` | `OverdueFollowUpScheduler` (cada 15 min) | Crea Notification de seguimiento perdido |

**Serialización:** Jackson2JsonMessageConverter

**No hay otros producers/consumers** en el repositorio.

---

# 8. Docker

## Dockerfiles

| Archivo | Descripción |
|---------|-------------|
| `infra/docker/Dockerfile.spring-service` | Multi-stage Maven 3.9 + Temurin 21 → JRE Alpine. Cache `dependency:go-offline`, labels OCI, usuario `careflow`, `wget` para healthchecks Compose. Genérico para 6 servicios Java |
| `frontend/Dockerfile` | Multi-stage Node 20 Alpine con `output: standalone`. Build args `API_GATEWAY_URL`, `BUILD_VERSION`. Usuario `careflow`. EXPOSE 3000 |

## Convención de imágenes (Sprint 1)

```
careflow/<service-name>:<CAREFLOW_VERSION>
```

Ver `docs/docker/image-naming.md`.

## docker-compose.build.yml

Build de las 7 imágenes de aplicación sin levantar infra. Usado por `scripts/validate-docker-build.*` y CI futuro.

## .dockerignore

Estandarizado en los 6 servicios backend (`backend/*/.dockerignore`) y canónico en `infra/docker/spring-service.dockerignore`.

## docker-compose.yml (desarrollo local)

**Solo infraestructura** — aplicaciones corren en host.

| Servicio | Imagen | Puertos | Volúmenes |
|----------|--------|---------|-----------|
| postgres-auth | postgres:16 | 5433:5432 | postgres-auth-data |
| postgres-patient | postgres:16 | 5434:5432 | postgres-patient-data |
| postgres-clinic | postgres:16 | 5435:5432 | postgres-clinic-data |
| postgres-followup | postgres:16 | 5436:5432 | postgres-followup-data |
| postgres-notification | postgres:16 | 5437:5432 | postgres-notification-data |
| rabbitmq | rabbitmq:3-management | 5672, 15672 | — |

**Red:** bridge default (sin red nombrada).

## docker-compose.staging.yml (VPS)

**Stack completo** — project name `careflow-staging`.

| Servicio | Imagen | Puertos expuestos | Límite RAM | Healthcheck |
|----------|--------|-------------------|------------|-------------|
| postgres | postgres:16-alpine | interno | 768M | pg_isready |
| rabbitmq | rabbitmq:3-management-alpine | interno | 384M | rabbitmq-diagnostics |
| auth/clinic/patient/followup/notification/gateway | `careflow/*:VERSION` | interno | 512M c/u | Actuator `/actuator/health` |
| frontend | `careflow/frontend:VERSION` | interno | 512M | HTTP `/` |
| nginx | nginx:1.27-alpine | **80, 443** | 128M | — |

**Red:** `careflow` (todos los servicios).

**Volúmenes:**
- `postgres-data` — datos PostgreSQL staging
- Bind mounts: nginx config, certbot www/conf

**Cadena de dependencias:**
```
postgres (healthy) → servicios JPA
rabbitmq (healthy) → followup, notification
patient-service → followup, notification
microservicios → api-gateway → frontend → nginx
```

## Imágenes base

- `postgres:16` / `postgres:16-alpine`
- `rabbitmq:3-management` / `rabbitmq:3-management-alpine`
- `maven:3.9-eclipse-temurin-21` (build)
- `eclipse-temurin:21-jre-alpine` (runtime Java)
- `node:20-alpine` (frontend)
- `nginx:1.27-alpine`
- `certbot/certbot`, `alpine` (scripts SSL)

---

# 9. Configuración

## Variables de entorno

### Raíz — `.env.example`

| Variable | Consumidores |
|----------|--------------|
| `CAREFLOW_JWT_SECRET` | api-gateway, auth-service |
| `CAREFLOW_INTERNAL_API_KEY` | auth-service, clinic-service |

### Staging — `infra/docker/.env.staging.example`

| Variable | Consumidores |
|----------|--------------|
| `POSTGRES_USER` | postgres + SPRING_DATASOURCE_USERNAME |
| `POSTGRES_PASSWORD` | postgres + SPRING_DATASOURCE_PASSWORD |
| `CAREFLOW_JWT_SECRET` | auth-service, api-gateway |
| `CAREFLOW_INTERNAL_API_KEY` | auth-service, clinic-service |

### Frontend — `frontend/.env.example`

| Variable | Uso |
|----------|-----|
| `API_GATEWAY_URL` | Rewrite Next.js `/api/*` → gateway |

## application.yml — patrón por servicio

| Servicio | Local | Profile `docker` |
|----------|-------|------------------|
| api-gateway | Rutas a localhost:8081-8085 | Hostnames Docker (`auth-service:8081`, etc.) |
| auth-service | localhost:5433/auth_db, clinic localhost:8083 | postgres:5432, clinic-service:8083 |
| patient-service | localhost:5434 | postgres:5432 |
| clinic-service | localhost:5435 | postgres:5432 |
| followup-service | localhost:5436, rabbitmq localhost | postgres + rabbitmq hostnames |
| notification-service | localhost:5437, rabbitmq localhost | postgres + rabbitmq hostnames |

## Perfiles Spring

| Entorno | Profile |
|---------|---------|
| Desarrollo (host) | default |
| Staging Docker | `docker` (`SPRING_PROFILES_ACTIVE=docker`) |
| Tests | `application-test.yml` (auth-service, api-gateway) |

## Configuración compartida (`careflow.*`)

```
careflow.jwt.secret
careflow.internal.api-key
careflow.clinic-service.url
careflow.patient-service.base-url
careflow.rabbitmq.exchange / .queue
careflow.followup.overdue-check-cron
careflow.invite.expiration-days
```

## Configuración hardcodeada (solo dev)

- PostgreSQL: usuario/contraseña `careflow/careflow`
- RabbitMQ: `guest/guest`
- JWT secret default en YAML (32+ chars repetidos)

---

# 10. Dependencias

## Backend — dependencias comunes (5 servicios con BD)

| Dependencia | Versión / gestión |
|-------------|-------------------|
| spring-boot-starter-parent | 3.5.14 |
| Java | 21 |
| spring-boot-starter-web | BOM Spring Boot |
| spring-boot-starter-data-jpa | BOM |
| spring-boot-starter-security | BOM |
| spring-boot-starter-validation | BOM |
| spring-boot-starter-actuator | BOM |
| postgresql (driver) | BOM runtime |
| lombok | optional |
| spring-boot-starter-test | test |

## api-gateway (adicional)

| Dependencia | Versión |
|-------------|---------|
| spring-cloud-dependencies | 2025.0.2 |
| spring-cloud-starter-gateway-server-webflux | BOM |
| jjwt-api/impl/jackson | 0.12.5 |
| wiremock-standalone | 3.9.1 (test) |
| reactor-test | test |

## auth-service (adicional)

| Dependencia | Versión |
|-------------|---------|
| jjwt-api/impl/jackson | 0.12.5 |
| testcontainers (junit-jupiter, postgresql) | test |

## followup-service (adicional)

| Dependencia | Versión |
|-------------|---------|
| spring-boot-starter-amqp | BOM |

## notification-service (adicional)

| Dependencia | Versión |
|-------------|---------|
| spring-boot-starter-amqp | BOM |

## Frontend — dependencias principales

| Paquete | Versión |
|---------|---------|
| next | 16.2.6 |
| react / react-dom | 19.2.4 |
| axios | ^1.16.1 |
| @tanstack/react-query | ^5.100.11 |
| zustand | ^5.0.13 |
| jwt-decode | ^4.0.0 |
| react-hook-form | ^7.76.0 |
| zod | ^4.4.3 |
| tailwindcss | ^4 |
| typescript | ^5 |

**Nota:** No existe parent POM Maven agregador — 6 `pom.xml` independientes con mismas versiones duplicadas.

---

# 11. Flujo de una petición (login)

## Secuencia paso a paso

```
1. Usuario abre https://app.careflowhq.org/login
   └── Nginx → frontend Next.js sirve página login

2. Usuario envía email + password
   └── React Hook Form → authApi.login() → axios POST /api/auth/login

3. Next.js rewrite (o Nginx en staging)
   └── /api/auth/login → api-gateway:8080/api/auth/login

4. API Gateway — SecurityConfig
   └── Ruta /api/auth/login está en permitAll() → no requiere JWT

5. Gateway routing (StripPrefix=1)
   └── Forward POST http://auth-service:8081/auth/login

6. auth-service — AuthController.login()
   └── AuthService.login():
       a. Busca User por email en auth_db
       b. BCrypt.matches(password, passwordHash)
       c. JwtService.generateToken(userId, clinicId, role)
       d. Retorna { "token": "<JWT>" }

7. Respuesta HTTP 200
   └── Gateway → Nginx → Frontend

8. Frontend auth-store
   └── setToken(token) → Zustand persist → localStorage "careflow-auth"

9. Redirect a /dashboard
   └── AuthGuard: isAuthenticated() decodifica JWT, verifica exp
   └── AppShell renderiza navegación según role

10. Petición subsiguiente (ej. GET /api/patients)
    a. axios interceptor añade Authorization: Bearer <JWT>
    b. Gateway JwtAuthenticationFilter valida firma y exp
    c. TenantIdentityPropagationFilter:
       - Elimina X-User-Id/X-Clinic-Id/X-Role del cliente
       - Inyecta valores desde JWT
    d. Proxy GET http://patient-service:8082/patients
    e. patient-service TenantContextFilter parsea headers
    f. PatientService.findAllForCurrentClinic() filtra por clinicId
    g. JSON response → frontend React Query cache
```

---

# 12. Flujo de creación de paciente

```
1. Usuario autenticado en /patients (CLINIC_ADMIN, DOCTOR o ASSISTANT)
   └── Formulario: fullName, phoneNumber, diagnosis, status, assignedDoctorId

2. Frontend patientsApi.create()
   └── POST /api/patients + Bearer JWT
       Body: CreatePatientRequest (sin clinicId)

3. API Gateway
   └── Valida JWT → inyecta X-User-Id, X-Clinic-Id, X-Role

4. patient-service — PatientController.create()
   └── TenantContextFilter ya pobló TenantContext desde headers

5. PatientService.create()
   a. clinicId = TenantContext.clinicId()  ← del JWT, no del body
   b. Construye entidad Patient (status default ACTIVE)
   c. patientRepository.save() → patient_db.patients
   d. Retorna PatientResponse (id, campos, createdAt)

6. Frontend
   └── React Query invalidateQueries → lista actualizada en UI
```

**Validaciones:** Bean Validation en DTOs; tenant isolation en repository (`findByIdAndClinicId`).

**No hay eventos** publicados al crear paciente.

---

# 13. Flujo de FollowUp

## 13.1 Creación de seguimiento

```
1. Usuario en /followups selecciona paciente, tipo, fecha, notas
   └── POST /api/followups

2. Gateway → followup-service con headers tenant

3. FollowUpService.create()
   a. Construye FollowUp (status PENDING, createdBy = userId)
   b. Persiste en followup_db
   c. PatientServiceClient.getPatient(patientId) → HTTP GET patient-service
      (propaga X-Clinic-Id, X-User-Id, X-Role)
   d. FollowUpEventPublisher.publishScheduled(followUp, patient)
      → RabbitMQ exchange careflow.events, routing key followup.scheduled

4. notification-service — NotificationEventListener.onCareFlowEvent()
   a. NotificationService.handleEvent()
   b. Idempotencia: skip si eventId existe
   c. NotificationMessageBuilder → mensaje es-PE
   d. WhatsAppLinkBuilder → wa.me link
   e. Persiste Notification (status READY, channel WHATSAPP_LINK)

6. Usuario ve notificación en /notifications
   └── Puede abrir WhatsApp y POST /api/notifications/{id}/send (mark SENT)
```

## 13.2 Completar seguimiento

```
PATCH /api/followups/{id}/complete
→ FollowUpService.complete()
→ status PENDING → COMPLETED
→ No publica evento RabbitMQ
```

## 13.3 Cancelar seguimiento

```
DELETE /api/followups/{id}
→ FollowUpService.cancel()
→ status PENDING → CANCELLED
→ No publica evento
```

## 13.4 Seguimiento vencido (automático)

```
OverdueFollowUpScheduler (@Scheduled cron cada 15 min)
→ findByStatusAndScheduledDateBefore(PENDING, now)
→ Para cada follow-up:
   a. status → MISSED
   b. PatientServiceClient.getPatient() (role hardcoded "CLINIC_ADMIN")
   c. FollowUpEventPublisher.publishMissed()
→ notification-service crea Notification followup.missed
```

---

# 14. Estado de implementación

| Funcionalidad | Estado | Comentarios |
|---------------|--------|-------------|
| Registro consultorio (CLINIC_ADMIN) | ✅ Implementado | Integración auth → clinic internal API |
| Login / JWT | ✅ Implementado | 24h, HS256, sin refresh token |
| Invitación staff | ✅ Implementado | DOCTOR/ASSISTANT, token 7 días |
| Aceptar invitación | ✅ Implementado | `/register-invite` |
| CRUD clínicas | ✅ Implementado | RBAC básico PLATFORM_ADMIN/CLINIC_ADMIN |
| CRUD pacientes | ✅ Implementado | Tenant isolation por clinicId |
| CRUD seguimientos | ✅ Implementado | Complete, cancel, pending list |
| Scheduler overdue → MISSED | ✅ Implementado | Cron 15 min |
| Notificaciones RabbitMQ | ✅ Implementado | scheduled + missed |
| WhatsApp demo (wa.me) | ✅ Implementado | Canal WHATSAPP_LINK |
| API Gateway + routing | ✅ Implementado | StripPrefix, 5 rutas |
| Frontend MVP (español) | ✅ Implementado | 6 pantallas app + auth |
| Docker Compose local | ✅ Implementado | Infra only + scripts host |
| Docker Compose staging | ✅ Implementado | Stack completo |
| Deploy VPS Hetzner | ✅ Implementado | CPX32, 178.105.118.30 |
| HTTPS Let's Encrypt | ✅ Implementado | app + root + www |
| Renovación SSL automática | ✅ Implementado | Cron diario 03:00 UTC |
| RBAC granular DOCTOR/ASSISTANT | ❌ No implementado | Todos acceden CRUD clínico |
| PLATFORM_ADMIN operativo | 🟡 Parcial | Enum existe, sin seed/UI |
| Migraciones BD versionadas | ✅ Implementado (Sprint 2) | Flyway V1 baseline + validate |
| shared-libs | ❌ Vacío | ADR 0004 no ejecutado |
| Refresh token | ❌ No implementado | — |
| Middleware Next.js auth | ❌ No implementado | Solo AuthGuard client-side |
| WhatsApp API real | ❌ No implementado | Solo links demo |
| Observabilidad (metrics/tracing) | ❌ No implementado | Solo actuator health/info |
| CI/CD pipeline | ❌ No implementado | Deploy manual script |
| Tests integración completos | 🟡 Parcial | auth + gateway; resto smoke |
| Email invitación automático | ❌ No implementado | Token manual/WhatsApp |
| Evento staff.invited | ❌ No implementado | Documentado como futuro |
| Landing separada de app | ❌ No implementado | Misma app en raíz y app subdomain |

---

# 15. Calidad de código

## Duplicación detectada

| Patrón duplicado | Copias | Ubicaciones |
|------------------|--------|-------------|
| `TenantContext` + `TenantIdentity` | 5 | auth, clinic, patient, followup, notification |
| `TenantHeaders` (constantes X-*) | 5 | mismos servicios |
| `TenantContextFilter` | 5 | ~67-80 líneas c/u, lógica casi idéntica |
| `MissingTenantContextException` | 5 | — |
| `GlobalExceptionHandler` | 5 | — |
| `SecurityConfig` (permitAll pattern) | 4 servicios + gateway distinto | — |
| `CareFlowEvent` record | 2 | followup-service, notification-service |
| Bloque JPA/datasource YAML | 5 | application.yml estructura idéntica |
| 6 pom.xml independientes | 6 | Sin parent aggregator |

**Estimación:** ~400+ líneas duplicadas en capa multi-tenant. `backend/shared-libs/` existe vacío.

## Clases muy grandes

**Ningún archivo de código supera 300 líneas** en todo el repositorio.

Archivos más grandes:
| Líneas | Archivo |
|--------|---------|
| ~239 | `frontend/src/app/(app)/followups/page.tsx` |
| ~220 | `frontend/src/app/(app)/patients/page.tsx` |
| ~170 | `frontend/src/app/(app)/dashboard/page.tsx` |
| ~102 | `backend/followup-service/.../FollowUpService.java` |

## Posibles code smells

| Smell | Ubicación | Descripción |
|-------|-----------|-------------|
| Trust boundary en headers | Todos los microservicios | Confianza ciega en headers gateway sin JWT propio |
| Role hardcoded en scheduler | `OverdueFollowUpScheduler` | Usa `"CLINIC_ADMIN"` string al llamar patient-service |
| Config huérfana | notification-service YAML | `patient-service.base-url` sin cliente |
| Secret default en YAML | auth + gateway | Mismo secret dev commiteado como default |
| JWT en localStorage | frontend auth-store | Superficie XSS |
| ddl-auto: update | 5 servicios | Riesgo schema drift en producción |
| Sin parent POM | backend/ | Versiones duplicadas, drift potencial |
| Páginas CRUD monolíticas | patients/page, followups/page | UI + lógica + forms en un solo archivo |

## Acoplamiento

| Tipo | Detalle | Nivel |
|------|---------|-------|
| Síncrono runtime | followup → patient (RestClient) | Medio |
| Síncrono runtime | auth → clinic (RestClient + API key) | Medio |
| Asíncrono | followup → notification (RabbitMQ) | Bajo |
| Temporal | Scheduler consulta todos los PENDING globalmente (sin filtro clinicId en query inicial) | Medio |
| Deploy | docker-compose.staging depends_on encadenado | Medio |
| Datos | UUID cross-DB sin FK ni consistencia transaccional | Alto (eventual consistency implícita) |

## Complejidad

- **Backend:** Baja — servicios delgados, capa service + repository, pocos servicios >100 líneas
- **Frontend:** Media en páginas CRUD — formularios, queries y UI mezclados
- **Infra:** Media — dos modelos Postgres (5 contenedores local vs 1 staging)
- **Seguridad:** Media-alta deuda — modelo correcto en gateway pero frágil si se expone puerto backend

---

# 16. Preparación para modernización

> Identificación de puntos de aplicación **sin proponer cambios**.

## Dónde sería posible aplicar Clean Architecture

| Área | Capas actuales | Oportunidad CA |
|------|----------------|----------------|
| auth-service | Controller → Service → Repository → Entity | Dominio Identity/Invite separable de infra JPA y ClinicServiceClient |
| patient-service | Idem | Agregado Patient con reglas de negocio (status transitions) aislables |
| followup-service | Idem + messaging + scheduler | Casos de uso CreateFollowUp, MarkMissed como application services; ports para PatientClient y EventPublisher |
| notification-service | Idem + listener | Handler de eventos como use case; adapters AMQP/JPA |
| clinic-service | Idem + internal API | Domain Clinic + policies de acceso |
| api-gateway | Filtros + routing | Menos aplicable (infra cross-cutting); posible módulo auth como adapter |
| frontend | pages + lib/api | Separación presentation / application / infrastructure parcialmente aplicable |

## Dónde podrían usarse Virtual Threads

| Componente | Motivo |
|------------|--------|
| auth-service | RestClient bloqueante a clinic-service en registro |
| followup-service | RestClient a patient-service en create + scheduler batch |
| patient/clinic/notification services | Servlet stack con I/O JDBC — beneficio en concurrencia bajo carga |
| api-gateway | WebFlux reactivo — virtual threads **menos aplicables** (modelo ya no bloqueante) |

## Servicios fáciles de contenerizar para Kubernetes

| Servicio | Facilidad | Notas |
|----------|-----------|-------|
| patient-service | Alta | Stateless, 1 BD, sin messaging |
| clinic-service | Alta | Stateless, 1 BD, 1 endpoint interno |
| auth-service | Alta | Stateless, 1 dependencia HTTP |
| notification-service | Media-Alta | Consumer RabbitMQ — requiere readiness probe en cola |
| followup-service | Media | Scheduler + RabbitMQ + HTTP client — considerar single replica o leader election |
| api-gateway | Alta | Stateless, ideal como Deployment con HPA |
| frontend | Alta | Stateless Next.js standalone |
| postgres (staging actual) | Baja como Pod único | Requiere StatefulSet + PVC por instancia o operador |
| rabbitmq | Media | StatefulSet o servicio managed |

## Componentes que necesitarán ConfigMaps

| Config | Contenido típico |
|--------|------------------|
| api-gateway | Rutas, actuator exposure, CORS (futuro) |
| auth-service | clinic-service.url, invite.expiration-days |
| followup-service | overdue-check-cron, rabbitmq.exchange, patient-service.base-url |
| notification-service | rabbitmq.exchange, rabbitmq.queue |
| clinic/patient-service | JPA show-sql off, logging levels |
| nginx | default.conf / ssl config (o Ingress controller) |
| frontend | API_GATEWAY_URL (si no via Ingress) |

## Componentes que necesitarán Secrets

| Secret | Consumidores |
|--------|--------------|
| CAREFLOW_JWT_SECRET | api-gateway, auth-service |
| CAREFLOW_INTERNAL_API_KEY | auth-service, clinic-service |
| POSTGRES_USER/PASSWORD | Todos los servicios JPA |
| SPRING_DATASOURCE URLs | Por servicio (o un secret por BD) |
| RabbitMQ credentials | followup-service, notification-service |
| TLS certificates | nginx/Ingress (actualmente certbot bind mount) |

---

# 17. Riesgos técnicos (despliegue Kubernetes)

| Riesgo | Severidad | Descripción |
|--------|-----------|-------------|
| Schema sin migraciones | Alta | `ddl-auto: update` en K8s multi-replica puede causar race conditions y schema impredecible |
| Microservicios sin JWT propio | Alta | Si Service expone puerto cluster-internal sin NetworkPolicy estricta, bypass de auth posible |
| JWT secret compartido | Alta | Compromiso del secret invalida todo el sistema; rotación requiere coordinación |
| RabbitMQ guest/guest | Alta | Credenciales default inaceptables en cluster compartido |
| Scheduler sin leader election | Media | Múltiples réplicas followup-service ejecutarían scheduler duplicado → eventos MISSED duplicados (mitigado parcialmente por idempotencia en notification) |
| Postgres single instance staging | Media | Modelo actual 1 Postgres/5 DB no escala igual en K8s que Cloud SQL/operador |
| Divergencia local vs staging DB | Media | 5 contenedores local vs 1 multi-DB staging — comportamiento distinto |
| Sin health probes custom | Media | Solo actuator; RabbitMQ consumer lag no reflejado en readiness |
| Sin distributed tracing | Media | Debugging cross-service difícil en K8s |
| Frontend JWT localStorage | Media | XSS en pod comprometido = robo de sesión |
| Certbot bind mount | Media | Renovación SSL acoplada a filesystem VPS; en K8s requiere cert-manager |
| Sin resource limits en local | Baja | Staging sí tiene limits; K8s necesita requests/limits por servicio |
| CareFlowEvent duplicado | Baja | Drift de contrato entre followup y notification al evolucionar |
| OverdueFollowUpScheduler query global | Media | `findByStatusAndScheduledDateBefore` sin paginación — carga creciente |
| Sin CI/CD | Media | Deploy manual propenso a errores humanos |
| shared-libs vacío | Baja | Duplicación aumentará deuda al escalar equipo |

---

# 18. Resumen ejecutivo

**CareFlow Platform** es un monorepo MVP funcional de una plataforma SaaS multi-tenant para clínicas privadas. Combina **6 microservicios Spring Boot 3 / Java 21**, un **API Gateway** con JWT, **5 bases PostgreSQL** (database-per-service), **RabbitMQ** para notificaciones asíncronas, y un **frontend Next.js 16** en español. El producto cubre el ciclo principal: registro de consultorio, login, gestión de pacientes, seguimientos clínicos con detección automática de vencidos, y notificaciones demo vía links WhatsApp.

**Fortalezas:** arquitectura de dominios bien delimitada en documentación; servicios pequeños y legibles (ninguna clase >300 líneas); separación clara gateway/servicios; tenant isolation consistente por `clinicId`; stack moderno (Java 21, Spring Boot 3.5, Next.js 16); infra de staging operativa con HTTPS y renovación automática; scripts de desarrollo y deploy documentados.

**Debilidades críticas para escalar:** ausencia total de migraciones de base de datos; microservicios que confían en headers HTTP sin validar JWT; duplicación masiva de código multi-tenant sin librería compartida; RBAC incompleto (roles DOCTOR/ASSISTANT sin restricciones); tests limitados fuera de auth/gateway; observabilidad mínima; secretos con defaults en configuración; frontend con auth solo client-side.

**Estado operativo:** demo local y staging en producción (`https://app.careflowhq.org`) verificados end-to-end. El proyecto está en fase **MVP avanzado**, listo para uso controlado pero **no production-hardened** para carga, seguridad enterprise, ni despliegue Kubernetes sin trabajo previo en migraciones, secrets, network policies, y observabilidad.

**Recomendación de lectura para modernización:** este documento sirve como baseline. Las áreas de mayor impacto identificadas son: (1) persistencia versionada, (2) consolidación capa tenant/shared-lib, (3) hardening seguridad servicio-a-servicio, (4) observabilidad, (5) alineación infra local/staging/K8s. Las secciones 16 y 17 mapean puntos de contacto concretos sin prescribir solución.

---

*Documento generado por auditoría estática del repositorio. No implica cambios al código fuente.*
