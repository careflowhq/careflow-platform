# Demo local — CareFlow MVP

Guía para mostrar el producto en tu máquina (inversor, cliente piloto o revisión interna).

## ¿Qué puedes demostrar hoy?

| Flujo | UI web | Notas |
|-------|--------|-------|
| Login CLINIC_ADMIN | ✅ `/login` | Requiere usuario ya creado |
| Dashboard (métricas) | ✅ `/dashboard` | Pacientes activos, seguimientos pendientes/vencidos |
| CRUD pacientes | ✅ `/patients` | Estados en español (Activo, En riesgo, Inactivo) |
| CRUD seguimientos | ✅ `/followups` | Crear, completar, cancelar; tipos en español |
| Invitar doctor/asistente | ✅ `/team` | Token manual (sin email automático aún) |
| Aceptar invitación | ✅ `/register-invite` | Pegar token + contraseña |
| **Alta de consultorio nuevo** | ✅ `/register` | Self-service; crea CLINIC_ADMIN + clínica |
| **Demo pública (URL)** | ❌ | Requiere deploy staging (pendiente) |

## Checklist antes de la demo

### 1. Infraestructura

```bash
cd infra/docker
docker compose up -d
```

Verificar contenedores: PostgreSQL (5433–5436) y RabbitMQ (5672, management 15672).

### 2. Backend (5 servicios)

En terminales separadas, desde cada módulo:

| Servicio | Puerto | Comando típico |
|----------|--------|----------------|
| api-gateway | 8080 | `mvn spring-boot:run` |
| auth-service | 8081 | `mvn spring-boot:run` |
| clinic-service | 8083 | `mvn spring-boot:run` |
| patient-service | 8082 | `mvn spring-boot:run` |
| followup-service | 8084 | `mvn spring-boot:run` |

Variables opcionales (ver `.env.example` en la raíz):

- `CAREFLOW_JWT_SECRET` (mín. 32 caracteres)
- `CAREFLOW_INTERNAL_API_KEY`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir [http://localhost:3000](http://localhost:3000). El proxy envía `/api/*` al gateway `:8080`.

### 4. Usuario de demo

**Opción A — Usuario existente:** login con credenciales ya registradas (ej. `maria@test.com`).

**Opción B — Consultorio nuevo (Postman):**

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "fullName": "Admin Demo",
  "email": "demo@consultorio.com",
  "password": "Demo1234!",
  "role": "CLINIC_ADMIN",
  "clinicName": "Consultorio Demo",
  "country": "PE",
  "timezone": "America/Lima"
}
```

Luego login en la web con ese email y contraseña.

Colección completa: [docs/api/careflow-smoke.postman_collection.json](../api/careflow-smoke.postman_collection.json).

---

## Guión sugerido (15 min)

1. **Registrar consultorio** — `/register` (nombre consultorio, admin, email).
2. **Dashboard** — métricas + bloque **Requiere atención** (vencidos, pacientes en riesgo).
3. **Pacientes** — crear paciente con teléfono y estado “En riesgo”.
4. **Seguimientos** — programar “Seguimiento post consulta” para mañana; marcar uno como completado.
5. **Equipo** — invitar doctor; copiar token y abrir `/register-invite` en ventana incógnito.
6. **Multi-tenant** — mismo backend, datos aislados por clínica (concepto, no pantalla admin).

### Narrativa de producto

> CareFlow ayuda a consultorios a no perder pacientes: seguimientos programados, alertas de vencidos y equipo con roles (admin, doctor, asistente).

---

## UI en español (LATAM)

Etiquetas centralizadas en `frontend/src/lib/labels.ts`:

- Roles: Administrador de clínica, Doctor, Asistente
- Estados paciente: Activo, En riesgo, Inactivo
- Estados seguimiento: Pendiente, Completado, Vencido, Cancelado
- Tipos de seguimiento: post consulta, recordatorio de cita, control de medicación, general

El backend sigue usando códigos en inglés (`CLINIC_ADMIN`, `PENDING`, etc.).

---

## Limitaciones conocidas (decir en la demo)

- Invitación por **token manual** (WhatsApp/correo propio); no hay envío automático.
- Todos los roles ven el mismo CRUD clínico (RBAC fino pendiente).
- Registro de consultorio nuevo **sin pantalla web** (solo API).
- Sin deploy staging: la demo es **solo local** por ahora.
- Breve “Cargando sesión…” al entrar (lectura de token en el navegador).

---

## Qué falta para demo “lista para mostrar”

Prioridad sugerida:

1. **Página `/register`** — alta de consultorio sin Postman
2. **Commit + merge del frontend** — código versionado en `main`
3. **Deploy staging** — URL pública (frontend + backend)
4. **Notification Service** — email/WhatsApp con token de invitación
5. Script único de arranque local (opcional, acelera preparación)

Ver [progress-log.md](../progress/progress-log.md) para estado detallado del proyecto.
