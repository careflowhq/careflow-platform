# CareFlow — scripts de arranque local

Documentación completa: [docs/deploy/local-development.md](../docs/deploy/local-development.md)

## Windows (desarrollo)

```powershell
# Desde la raíz del repo
.\scripts\start-local.ps1

# Detener infra Docker
.\scripts\stop-local.ps1
```

**Qué hace `start-local.ps1`:**
1. Levanta Docker (PostgreSQL ×5 + RabbitMQ)
2. Inicia los microservicios (8081–8085) y **espera** a que escuchen en sus puertos
3. Inicia **api-gateway** (:8080) al final y espera a que esté listo
4. Inicia el frontend Next.js en `:3000`

La primera vez Maven puede tardar **1–2 min por servicio**. El script espera automáticamente; no abras la app hasta ver `Listo`.

**Requisitos:** Docker Desktop, Java 21, Maven, Node.js 20+

## Linux / macOS

```bash
chmod +x scripts/*.sh
./scripts/start-local.sh
./scripts/stop-local.sh
```

## URLs

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| RabbitMQ UI | http://localhost:15672 (guest/guest) |

## Variables de entorno

Opcional: copiar `.env.example` → `.env` en la raíz. Los scripts cargan `.env` si existe.

## Detener

```powershell
.\scripts\stop-local.ps1
```

`stop-local` ejecuta `docker compose down` **sin borrar volúmenes**: tus usuarios y datos en Postgres se conservan entre reinicios.

Para resetear todo desde cero (borra datos):

```powershell
cd infra\docker
docker compose down -v
```

## VPS (staging)

- Provisión servidor: [docs/deploy/hetzner-vps-setup.md](../docs/deploy/hetzner-vps-setup.md)
- Deploy aplicación: [docs/deploy/staging-vps.md](../docs/deploy/staging-vps.md)
- Índice: [docs/deploy/README.md](../docs/deploy/README.md)
- Script: `./scripts/deploy-staging.sh`
