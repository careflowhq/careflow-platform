# Convención de nombres de imágenes

## Formato local / staging

```
careflow/<service-name>:<version>
```

Ejemplos:

```
careflow/api-gateway:0.0.1-SNAPSHOT
careflow/auth-service:0.0.1-SNAPSHOT
careflow/frontend:0.0.1-SNAPSHOT
```

## Variable de versión

Definida en `infra/docker/.env.staging.example`:

```env
CAREFLOW_VERSION=0.0.1-SNAPSHOT
```

Usada por `docker-compose.staging.yml` y `docker-compose.build.yml`.

## Formato Azure Container Registry (Sprint 4)

Cuando se despliegue en AKS, el prefijo del registry se añadirá:

```
<acr-name>.azurecr.io/careflow/auth-service:0.0.1-SNAPSHOT
```

Ejemplo con registry `careflowhq`:

```
careflowhq.azurecr.io/careflow/auth-service:0.0.1-SNAPSHOT
```

## Labels OCI

Cada imagen incluye:

| Label | Ejemplo |
|-------|---------|
| `org.opencontainers.image.title` | `CareFlow auth-service` |
| `org.opencontainers.image.version` | `0.0.1-SNAPSHOT` |
| `org.opencontainers.image.vendor` | `CareFlow HQ` |
| `careflow.service` | `auth-service` |

## Tags recomendados

| Tag | Cuándo usar |
|-----|-------------|
| `0.0.1-SNAPSHOT` | Desarrollo en rama feature |
| `1.0.0` | Release estable |
| `1.0.0-<git-sha>` | Trazabilidad en CI (Sprint 10) |
| `latest` | Solo entornos locales opcionales — evitar en producción |

## Servicios y puertos

| Imagen | Puerto | Healthcheck |
|--------|--------|-------------|
| `careflow/api-gateway` | 8080 | `/actuator/health` |
| `careflow/auth-service` | 8081 | `/actuator/health` |
| `careflow/patient-service` | 8082 | `/actuator/health` |
| `careflow/clinic-service` | 8083 | `/actuator/health` |
| `careflow/followup-service` | 8084 | `/actuator/health` |
| `careflow/notification-service` | 8085 | `/actuator/health` |
| `careflow/frontend` | 3000 | HTTP `/` |
