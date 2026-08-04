# Build y validación de imágenes Docker

## Requisitos

- Docker Desktop o Docker Engine 24+
- Docker Compose v2
- ~4 GB RAM libre para builds paralelos (recomendado build secuencial en máquinas limitadas)

## Build rápido (validación Sprint 1)

Desde la raíz del repositorio:

```bash
# Linux / macOS / Git Bash
./scripts/validate-docker-build.sh
```

```powershell
# Windows
.\scripts\validate-docker-build.ps1
```

Equivalente manual:

```bash
cd infra/docker
docker compose -f docker-compose.build.yml build
```

## Build de un solo servicio

```bash
cd infra/docker
docker compose -f docker-compose.build.yml build auth-service
```

## Build con versión personalizada

```bash
cd infra/docker
CAREFLOW_VERSION=1.0.0-rc1 docker compose -f docker-compose.build.yml build
```

Windows PowerShell:

```powershell
$env:CAREFLOW_VERSION = "1.0.0-rc1"
docker compose -f docker-compose.build.yml build
```

## Staging completo (VPS)

```bash
cd infra/docker
cp .env.staging.example .env   # editar secrets
docker compose -f docker-compose.staging.yml up -d --build
```

## Verificar imágenes construidas

```bash
docker images | grep careflow
```

Salida esperada (7 imágenes):

```
careflow/api-gateway
careflow/auth-service
careflow/clinic-service
careflow/patient-service
careflow/followup-service
careflow/notification-service
careflow/frontend
```

## Verificar healthchecks (stack levantado)

```bash
cd infra/docker
docker compose -f docker-compose.staging.yml ps
```

Estado `healthy` en servicios Java y frontend tras ~90 s de arranque.

Probar Actuator manualmente:

```bash
docker exec careflow-auth wget -qO- http://localhost:8081/actuator/health
```

## Desarrollo local (sin containerizar apps)

El flujo local **no cambia** en Sprint 1:

```bash
# Infra en Docker
cd infra/docker && docker compose up -d

# Apps en host
./scripts/start-local.sh   # o start-local.ps1
```

## Troubleshooting

### Build Maven lento la primera vez

Normal: descarga dependencias. La segunda ejecución usa cache de capas Docker (`dependency:go-offline`).

### Frontend: error `standalone` no encontrado

Verificar que `frontend/next.config.ts` incluye `output: "standalone"`.

### Healthcheck falla en api-gateway (unhealthy)

El gateway usa Spring Security. Si `/actuator/health` devuelve **401**, el healthcheck de Compose falla aunque la app funcione.

Verificar:

```bash
docker exec careflow-gateway wget -S -O- http://localhost:8080/actuator/health
```

Solución aplicada en `SecurityConfig`: permitir `/actuator/health/**` sin autenticación (requerido también para probes de Kubernetes).

### Healthcheck falla en otros servicios Java

- Esperar `start_period: 90s` en el primer arranque (JPA + ddl-auto)
- Revisar logs: `docker compose -f docker-compose.staging.yml logs auth-service`

### Puerto incorrecto en healthcheck

Cada servicio expone su puerto fijo en `application.yml`. Los healthchecks en Compose usan el puerto correspondiente (8080–8085).
