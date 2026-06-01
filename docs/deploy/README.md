# CareFlow — Deploy e infraestructura

Documentación de entornos: desarrollo local, staging en VPS y decisiones de infraestructura.

| Documento | Contenido |
|-----------|-----------|
| [local-development.md](./local-development.md) | Scripts de arranque, Docker local, persistencia de datos |
| [hetzner-vps-setup.md](./hetzner-vps-setup.md) | Provisión del servidor Hetzner, SSH, firewall, bootstrap |
| [staging-vps.md](./staging-vps.md) | Deploy del stack completo con Docker Compose |
| [ssl-careflowhq.md](./ssl-careflowhq.md) | HTTPS Let's Encrypt para careflowhq.org |

## Resumen de entornos

| Entorno | Dónde corre | Compose / script | URL |
|---------|-------------|------------------|-----|
| **Local (dev)** | Windows / Linux dev | `infra/docker/docker-compose.yml` + `scripts/start-local.*` | http://localhost:3000 |
| **Staging (VPS)** | Hetzner CPX32 | `infra/docker/docker-compose.staging.yml` + `scripts/deploy-staging.sh` | https://app.careflowhq.org · https://careflowhq.org |

## Archivos clave en el repo

```
infra/docker/
├── docker-compose.yml           # Dev: 5 Postgres + RabbitMQ
├── docker-compose.staging.yml   # Staging: stack completo
├── Dockerfile.spring-service    # Build genérico microservicios Java
├── .env.staging.example         # Secrets staging (copiar a .env)
├── nginx/default.conf           # Reverse proxy staging
└── postgres/init-databases.sh   # Crea 5 DB en Postgres único (staging)

scripts/
├── start-local.ps1 / .sh        # Arranque dev
├── stop-local.ps1 / .sh         # Detener infra Docker dev
└── deploy-staging.sh            # Build + up en VPS

backend/*/application-docker.yml   # Perfil Spring para red Docker
frontend/Dockerfile              # Build Next.js producción
```

## Secrets

| Variable | Uso | Dónde definir |
|----------|-----|---------------|
| `CAREFLOW_JWT_SECRET` | Firma JWT (gateway + auth) | `.env` local / `infra/docker/.env` staging |
| `CAREFLOW_INTERNAL_API_KEY` | auth ↔ clinic | idem |
| `POSTGRES_PASSWORD` | Postgres staging | `infra/docker/.env` |
| `API_GATEWAY_URL` | Proxy Next.js → gateway | `frontend/.env` local; build arg en Docker staging |

Ver `.env.example` (raíz) y `infra/docker/.env.staging.example`.
