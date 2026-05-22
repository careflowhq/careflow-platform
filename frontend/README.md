# CareFlow Web App

Frontend Next.js de CareFlow. Conecta al API Gateway vía proxy (`/api/*` → `localhost:8080`).

## Stack

- Next.js 16 (App Router)
- TypeScript
- Tailwind CSS v4
- TanStack Query
- Zustand (auth en `localStorage`)
- React Hook Form + Zod
- Axios

## Requisitos previos

Backend local en ejecución:

| Servicio | Puerto |
|----------|--------|
| API Gateway | 8080 |
| auth-service | 8081 |
| patient-service | 8082 |
| clinic-service | 8083 |
| followup-service | 8084 |
| PostgreSQL (Docker) | 5433–5436 |

Guía completa de demo: [docs/demo/local-demo.md](../docs/demo/local-demo.md)

## Arranque

```bash
cd frontend
cp .env.example .env.local   # opcional
npm install
npm run dev
```

Abrir [http://localhost:3000](http://localhost:3000)

## Pantallas MVP

| Ruta | Descripción |
|------|-------------|
| `/login` | Inicio de sesión |
| `/register-invite` | Aceptar invitación de staff |
| `/dashboard` | Métricas + próximos seguimientos |
| `/patients` | CRUD pacientes |
| `/followups` | Seguimientos: listar, crear, completar, cancelar |
| `/team` | Invitar doctor/asistente (solo CLINIC_ADMIN) |

> **Pendiente:** `/register` para alta de consultorio nuevo (hoy vía Postman).

## Flujo demo (con usuario existente)

1. Login como CLINIC_ADMIN
2. Crear paciente en **Pacientes**
3. Crear seguimiento en **Seguimientos**
4. Marcar como completado
5. **Equipo** → invitar doctor → copiar token → `/register-invite` en otra ventana

## UI en español

Etiquetas en `src/lib/labels.ts` (roles, estados, tipos de seguimiento). El backend conserva códigos en inglés.

## Variables de entorno

| Variable | Default | Uso |
|----------|---------|-----|
| `API_GATEWAY_URL` | `http://localhost:8080` | Target del proxy server-side |

Token JWT: `localStorage` key `careflow-auth`.

## Notas técnicas

- **AuthGuard:** espera montaje en cliente antes de leer sesión (evita error de hidratación SSR).
- Breve pantalla “Cargando sesión…” al entrar es comportamiento esperado.
