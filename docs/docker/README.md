# Docker — CareFlow Platform

Documentación de contenedores para la rama `feature/backend-modernization`.

## Contenido

| Documento | Descripción |
|-----------|-------------|
| [build-and-run.md](./build-and-run.md) | Comandos para construir y validar imágenes |
| [image-naming.md](./image-naming.md) | Convención de nombres y tags |

## Arquitectura de imágenes

```
careflow/
├── api-gateway          :8080
├── auth-service         :8081
├── patient-service      :8082
├── clinic-service       :8083
├── followup-service     :8084
├── notification-service :8085
└── frontend             :3000
```

## Dockerfiles

| Archivo | Uso |
|---------|-----|
| `infra/docker/Dockerfile.spring-service` | Plantilla genérica para los 6 microservicios Java |
| `frontend/Dockerfile` | Next.js con `output: standalone` |

## Compose files

| Archivo | Propósito |
|---------|-----------|
| `infra/docker/docker-compose.yml` | Infra local (Postgres + RabbitMQ) — apps en host |
| `infra/docker/docker-compose.staging.yml` | Stack completo para VPS staging |
| `infra/docker/docker-compose.build.yml` | Solo build de imágenes (CI / validación) |

## Estándares aplicados (Sprint 1)

- Multi-stage build (Maven/Node → runtime mínimo)
- Usuario non-root `careflow`
- Cache de dependencias Maven (`dependency:go-offline`)
- Labels OCI (`org.opencontainers.image.*`, `careflow.service`)
- Healthchecks en Compose vía Actuator (`/actuator/health`)
- Imágenes taggeadas con `CAREFLOW_VERSION`
- `.dockerignore` estandarizado en cada servicio backend

## Próximos sprints

| Sprint | Mejora Docker/K8s |
|--------|-------------------|
| 3 | Imágenes en Kind (local Kubernetes) |
| 4 | Push a Azure Container Registry |
| 10 | Build en Azure DevOps CI/CD |
