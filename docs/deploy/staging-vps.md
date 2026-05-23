# Deploy — CareFlow staging (VPS)

Guía para desplegar el stack completo en el VPS Hetzner.

**Prerrequisitos:** servidor provisionado según [hetzner-vps-setup.md](./hetzner-vps-setup.md).

## Servidor

| Campo | Valor |
|-------|-------|
| Host | `careflow-staging` |
| IP | `178.105.118.30` |
| Usuario SSH | `deploy` |
| RAM | 8 GB (CPX32) |

## Arquitectura staging

```
Internet :80
    └── nginx (careflow-nginx)
          ├── /      → frontend:3000 (Next.js)
          └── /api/* → api-gateway:8080
                              ├── auth-service:8081
                              ├── patient-service:8082
                              ├── clinic-service:8083
                              ├── followup-service:8084
                              └── notification-service:8085

Red interna Docker (careflow):
    postgres:5432     → auth_db, patient_db, clinic_db, followup_db, notification_db
    rabbitmq:5672     → eventos followup.scheduled / followup.missed
```

### Diferencias vs desarrollo local

| Aspecto | Local (dev) | Staging (VPS) |
|---------|-------------|---------------|
| Compose | `docker-compose.yml` | `docker-compose.staging.yml` |
| Postgres | 5 contenedores (:5433–5437) | 1 contenedor, 5 databases |
| Servicios Java | `mvn spring-boot:run` en host | Contenedores Docker (JRE 21) |
| Frontend | `npm run dev` | `npm run build` + `npm start` en contenedor |
| Entrada HTTP | :3000 directo | Nginx :80 |
| Perfil Spring | default | `docker` (`application-docker.yml`) |
| Secrets | `.env` dev | `infra/docker/.env` (fuerte) |

## Archivos del deploy

| Archivo | Rol |
|---------|-----|
| `infra/docker/docker-compose.staging.yml` | Orquestación completa |
| `infra/docker/Dockerfile.spring-service` | Multi-stage Maven → JRE 21 |
| `frontend/Dockerfile` | Build Next.js producción |
| `infra/docker/nginx/default.conf` | Reverse proxy |
| `infra/docker/postgres/init-databases.sh` | Crea las 5 bases al primer arranque |
| `infra/docker/.env.staging.example` | Plantilla de secrets |
| `backend/*/application-docker.yml` | URLs internas Docker |
| `scripts/deploy-staging.sh` | Script de deploy |

### Límites de memoria (compose)

| Servicio | Límite |
|----------|--------|
| postgres | 768 MB |
| rabbitmq | 384 MB |
| cada microservicio Java | 512 MB (heap `-Xmx384m`) |
| frontend | 512 MB |
| nginx | 128 MB |

## 1. Clonar el repo

```bash
ssh deploy@178.105.118.30

git clone https://github.com/careflowhq/careflow-platform.git
cd careflow-platform
chmod +x scripts/deploy-staging.sh
```

## 2. Configurar secrets

```bash
cd infra/docker
cp .env.staging.example .env
nano .env
```

Generar valores seguros:

```bash
openssl rand -hex 32   # CAREFLOW_JWT_SECRET
openssl rand -hex 24   # CAREFLOW_INTERNAL_API_KEY
openssl rand -hex 16   # POSTGRES_PASSWORD
```

Contenido de `.env`:

```env
POSTGRES_USER=careflow
POSTGRES_PASSWORD=<generado>
CAREFLOW_JWT_SECRET=<generado>
CAREFLOW_INTERNAL_API_KEY=<generado>
```

> `.env` está en `.gitignore` — nunca commitear.

## 3. Desplegar

Desde la raíz del repo:

```bash
./scripts/deploy-staging.sh
```

El script:

1. Verifica que `.env` existe y no contiene `change-me`
2. Ejecuta `docker compose -f docker-compose.staging.yml up -d --build`

**Primera vez:** 15–30 min (build de 6 JARs + frontend).

## 4. Verificar

```bash
cd ~/careflow-platform/infra/docker
docker compose -f docker-compose.staging.yml ps
docker compose -f docker-compose.staging.yml logs -f nginx
```

Navegador: **http://178.105.118.30**

- Registro: `/register`
- Login: `/login`

## Comandos útiles

```bash
cd ~/careflow-platform/infra/docker

# Logs de un servicio
docker compose -f docker-compose.staging.yml logs -f api-gateway
docker compose -f docker-compose.staging.yml logs -f followup-service

# Rebuild tras cambios
docker compose -f docker-compose.staging.yml up -d --build

# Reiniciar un servicio
docker compose -f docker-compose.staging.yml restart api-gateway

# Detener (conserva volumen Postgres)
docker compose -f docker-compose.staging.yml down

# Reset total — borra datos
docker compose -f docker-compose.staging.yml down -v
```

## Actualizar desde Git

```bash
ssh deploy@178.105.118.30
cd ~/careflow-platform
git pull
./scripts/deploy-staging.sh
```

## HTTPS con dominio (próximo paso)

1. Registro DNS **A** → `178.105.118.30`
2. Instalar Certbot en el host o extender Nginx
3. Actualizar `infra/docker/nginx/default.conf`:
   - `server_name app.tudominio.com;`
   - Bloque SSL `:443`
4. Renovar certificados automáticamente

Opcional: subdominios separados `app.` (frontend) y `api.` (gateway directo).

## Troubleshooting

### Build falla por memoria

En VPS 8 GB, si el build paralelo agota RAM:

```bash
docker compose -f docker-compose.staging.yml build auth-service
docker compose -f docker-compose.staging.yml build patient-service
# ... uno por uno
docker compose -f docker-compose.staging.yml up -d
```

### Contenedor reiniciando

```bash
docker compose -f docker-compose.staging.yml logs --tail=100 <servicio>
```

Causas frecuentes: Postgres no listo, RabbitMQ no listo, secret JWT distinto entre gateway y auth.

### 502 Bad Gateway en Nginx

Gateway o frontend aún no arrancaron. Esperar 2–3 min tras `up -d` o revisar logs de `api-gateway` y `frontend`.

## Referencias

- [README deploy](./README.md)
- [Setup Hetzner](./hetzner-vps-setup.md)
- [Desarrollo local](./local-development.md)
