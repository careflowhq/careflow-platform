# Desarrollo local — CareFlow

Guía del entorno de desarrollo en máquina local (Windows o Linux/macOS).

## Requisitos

| Herramienta | Versión |
|-------------|---------|
| Docker Desktop | Con Compose v2 |
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 20+ |

## Arranque rápido

### Windows (PowerShell)

Desde la **raíz del repo**:

```powershell
.\scripts\start-local.ps1
```

Detener infra Docker:

```powershell
.\scripts\stop-local.ps1
```

Si PowerShell bloquea scripts:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\start-local.ps1
```

### Linux / macOS

```bash
chmod +x scripts/*.sh
./scripts/start-local.sh
./scripts/stop-local.sh
```

## Qué hace `start-local`

1. Carga `.env` de la raíz si existe
2. `docker compose up -d` en `infra/docker/` (Postgres ×5 + RabbitMQ)
3. Inicia **microservicios** (8081–8085) en ventanas/background
4. **Espera** a que cada puerto responda (hasta 4 min por servicio)
5. Inicia **api-gateway** (:8080) al final y espera
6. Inicia **frontend** (`npm run dev` → :3000)

> Maven puede tardar 1–2 min por servicio la primera vez. No abras la app hasta ver `Listo`.

### Orden de arranque (importante)

El gateway **proxifica** al resto. Si arranca antes que patient/followup, verás errores `Connection refused` en el gateway. El script ya inicia el gateway **al final**.

## URLs locales

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| RabbitMQ UI | http://localhost:15672 (guest/guest) |

## Puertos

| Puerto | Servicio |
|--------|----------|
| 3000 | Frontend Next.js |
| 8080 | API Gateway |
| 8081 | auth-service |
| 8082 | patient-service |
| 8083 | clinic-service |
| 8084 | followup-service |
| 8085 | notification-service |
| 5433–5437 | PostgreSQL (auth, patient, clinic, followup, notification) |
| 5672 / 15672 | RabbitMQ AMQP / management UI |

## Docker Compose local

Archivo: `infra/docker/docker-compose.yml`

- **5 contenedores Postgres** (uno por bounded context), puertos 5433–5437
- **RabbitMQ** con UI en :15672
- **Volúmenes nombrados** para persistir datos entre reinicios

Los servicios Java **no** van en Docker en dev; se ejecutan con `mvn spring-boot:run` desde el script.

## Persistencia de datos

### Comportamiento

| Acción | ¿Conserva datos? |
|--------|------------------|
| Cerrar ventanas Java / reiniciar frontend | ✅ Sí (Postgres sigue corriendo) |
| `.\scripts\start-local.ps1` sin `stop` previo | ✅ Sí |
| `.\scripts\stop-local.ps1` (`docker compose down`) | ✅ Sí (volúmenes nombrados) |
| `docker compose down -v` | ❌ No — borra volúmenes |

### Por qué se perdieron datos antes

Sin volúmenes nombrados, los datos vivían **dentro del contenedor**. Al hacer `docker compose down`, los contenedores se eliminaban y las bases quedaban vacías.

Desde el commit `19f1e20` los volúmenes están definidos en `docker-compose.yml`.

### Reset limpio (dev)

```powershell
cd infra\docker
docker compose down -v
```

Luego registrar de nuevo en http://localhost:3000/register.

## Variables de entorno (local)

Copiar en la raíz del repo:

```bash
cp .env.example .env
```

| Variable | Descripción |
|----------|-------------|
| `CAREFLOW_JWT_SECRET` | Mín. 32 caracteres recomendado |
| `CAREFLOW_INTERNAL_API_KEY` | Clave auth → clinic |

Frontend (`frontend/.env`):

```
API_GATEWAY_URL=http://localhost:8080
```

## Troubleshooting

### PowerShell: "Falta la cadena en el terminador"

Causa: encoding UTF-8 con guiones Unicode (`—`) en scripts antiguos. Solución: usar scripts actuales en `scripts/` (ASCII + comillas simples).

### Gateway 500 — Connection refused a :8082 / :8084

El frontend llamó la API antes de que los microservicios estuvieran listos. Espera a `Listo` del script o reinicia solo el gateway después de que todos los Java estén arriba.

### Login "Invalid email or password" tras reiniciar Docker

Base de datos vacía. Registra de nuevo en `/register` o evita `docker compose down -v`.

### Servicios duplicados / puerto en uso

Cierra ventanas PowerShell de ejecuciones anteriores antes de volver a lanzar `start-local.ps1`.

## Demo guiada

Ver [docs/demo/local-demo.md](../demo/local-demo.md).
