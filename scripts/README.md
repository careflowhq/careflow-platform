# CareFlow — scripts de arranque local

## Windows (desarrollo)

```powershell
# Desde la raíz del repo
.\scripts\start-local.ps1

# Detener infra Docker
.\scripts\stop-local.ps1
```

**Qué hace `start-local.ps1`:**
1. Levanta Docker (PostgreSQL ×5 + RabbitMQ)
2. Abre una ventana por cada servicio Java (8080–8085)
3. Inicia el frontend Next.js en `:3000`

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

## VPS (próximo paso)

El deploy en producción usará Docker Compose completo (distinto a este arranque dev). Ver `docs/deploy/` (pendiente).
