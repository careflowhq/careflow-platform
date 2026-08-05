# Kubernetes — CareFlow Platform

Documentación de despliegue en Kubernetes (Sprint 3+).

## Contenido

| Documento | Descripción |
|-----------|-------------|
| [kind-quickstart.md](./kind-quickstart.md) | Cluster local Kind paso a paso |

## Estructura del repositorio

```
infra/
  kind/
    careflow-kind.yaml          # Config del cluster local
  kubernetes/
    base/                       # Infra compartida (namespace, postgres, rabbitmq)
      namespace.yaml
      postgres/
      rabbitmq/
      kustomization.yaml
scripts/
  kind-up.ps1 / kind-up.sh      # Crear cluster + desplegar infra
  kind-down.ps1 / kind-down.sh  # Eliminar cluster
```

## Sprint 3 — progreso

| Fase | Estado | Contenido |
|------|--------|-----------|
| 3.0 | ✅ | Kind cluster + scripts |
| 3.1 | ✅ | Namespace, Postgres, RabbitMQ |
| 3.2 | Pendiente | auth-service (primer microservicio) |
| 3.3 | Pendiente | Resto de microservicios + gateway |
| 3.4 | Pendiente | Frontend + Ingress (http://localhost:8088) |

## Servicios DNS internos (namespace `careflow`)

| Service | Puerto | Uso |
|---------|--------|-----|
| `postgres` | 5432 | Bases auth_db, patient_db, etc. |
| `rabbitmq` | 5672 | AMQP (followup, notification) |

Estos nombres coinciden con `application-docker.yml` para facilitar el despliegue de apps en fases siguientes.

## Próximos sprints

| Sprint | Mejora K8s |
|--------|------------|
| 4 | Azure Kubernetes Service (AKS) |
| 5 | ConfigMaps y Secrets unificados |
| 6 | Leader election (scheduler multi-réplica) |
| 7 | Observabilidad (Prometheus, Grafana, OTel) |
