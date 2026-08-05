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
    apps/                       # Microservicios (kustomize overlay)
      secrets.yaml              # careflow-app-secrets (JWT, internal API key)
      auth-service/
      clinic-service/
      patient-service/
      followup-service/
      notification-service/
      api-gateway/
      kustomization.yaml
scripts/
  kind-up.ps1 / kind-up.sh      # Crear cluster + desplegar infra
  kind-down.ps1 / kind-down.sh  # Eliminar cluster
  kind-deploy-auth.ps1/.sh      # Solo auth-service (3.2)
  kind-deploy-apps.ps1/.sh      # Backend completo (3.3)
```

## Sprint 3 — progreso

| Fase | Estado | Contenido |
|------|--------|-----------|
| 3.0 | ✅ | Kind cluster + scripts |
| 3.1 | ✅ | Namespace, Postgres, RabbitMQ |
| 3.2 | ✅ | auth-service (primer microservicio) |
| 3.3 | ✅ | Resto de microservicios + api-gateway |
| 3.4 | Pendiente | Frontend + Ingress (http://localhost:8088) |

## Servicios DNS internos (namespace `careflow`)

| Service | Puerto | Uso |
|---------|--------|-----|
| `postgres` | 5432 | Bases auth_db, patient_db, etc. |
| `rabbitmq` | 5672 | AMQP (followup, notification) |
| `auth-service` | 8081 | Autenticación |
| `patient-service` | 8082 | Pacientes |
| `clinic-service` | 8083 | Clínicas |
| `followup-service` | 8084 | Seguimientos |
| `notification-service` | 8085 | Notificaciones |
| `api-gateway` | 8080 | Entrada HTTP `/api/*` |

Estos nombres coinciden con `application-docker.yml` para facilitar el despliegue de apps en fases siguientes.

## Próximos sprints

| Sprint | Mejora K8s |
|--------|------------|
| 4 | Azure Kubernetes Service (AKS) |
| 5 | ConfigMaps y Secrets unificados |
| 6 | Leader election (scheduler multi-réplica) |
| 7 | Observabilidad (Prometheus, Grafana, OTel) |
