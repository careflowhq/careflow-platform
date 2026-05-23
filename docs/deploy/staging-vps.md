# Deploy — CareFlow staging (VPS)

Guía para desplegar CareFlow en el VPS Hetzner (`careflow-staging`).

## Requisitos en el servidor

- Ubuntu 24.04
- Docker + Docker Compose v2
- Usuario `deploy` en grupo `docker`
- Puertos **80** (y **443** cuando agregues HTTPS) abiertos en firewall

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

Generar valores seguros (en el VPS):

```bash
openssl rand -hex 32   # CAREFLOW_JWT_SECRET
openssl rand -hex 24   # CAREFLOW_INTERNAL_API_KEY
openssl rand -hex 16   # POSTGRES_PASSWORD
```

## 3. Desplegar

Desde la raíz del repo:

```bash
./scripts/deploy-staging.sh
```

La primera vez tarda **15–30 min** (build de 6 servicios Java + frontend).

## 4. Verificar

```bash
cd infra/docker
docker compose -f docker-compose.staging.yml ps
```

Abrir en el navegador: **http://178.105.118.30**

Registrar consultorio en `/register`.

## Arquitectura staging

```
Internet :80
    └── nginx
          ├── /      → frontend (Next.js)
          └── /api/* → api-gateway → microservicios
```

- **1 Postgres** con 5 bases (`auth_db`, `patient_db`, …)
- **RabbitMQ** interno (no expuesto)
- Perfil Spring **`docker`** (`application-docker.yml`)

## Comandos útiles

```bash
cd ~/careflow-platform/infra/docker

docker compose -f docker-compose.staging.yml logs -f api-gateway
docker compose -f docker-compose.staging.yml up -d --build
docker compose -f docker-compose.staging.yml down
docker compose -f docker-compose.staging.yml down -v   # borra datos
```

## HTTPS con dominio (próximo paso)

Cuando tengas `app.tudominio.com`:

1. Apuntar registro **A** al IP del VPS
2. Certbot + actualizar `infra/docker/nginx/default.conf`

## Actualizar

```bash
ssh deploy@178.105.118.30
cd ~/careflow-platform
git pull
./scripts/deploy-staging.sh
```
