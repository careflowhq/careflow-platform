# SSL — careflowhq.org

HTTPS con **Let's Encrypt** (Certbot) en el VPS Hetzner.

## Dominios

| Host | Uso |
|------|-----|
| **app.careflowhq.org** | URL principal de la app |
| **careflowhq.org** | Raíz → mismo sitio |
| **www.careflowhq.org** | Alias → mismo sitio |

Servidor: `178.105.118.30`

---

## Paso 1 — DNS (obligatorio antes de SSL)

En el panel de tu registrador (donde compraste `careflowhq.org`), crea registros **A**:

| Tipo | Nombre / Host | Valor | TTL |
|------|---------------|-------|-----|
| A | `app` | `178.105.118.30` | 300–3600 |
| A | `@` | `178.105.118.30` | 300–3600 |
| A | `www` | `178.105.118.30` | 300–3600 |

Espera propagación (5–60 min). Verifica en el VPS:

```bash
getent ahosts app.careflowhq.org
getent ahosts careflowhq.org
```

Debe mostrar `178.105.118.30`.

---

## Paso 2 — Actualizar repo en VPS

```bash
ssh deploy@178.105.118.30
cd ~/careflow-platform
git pull
chmod +x scripts/setup-ssl.sh scripts/renew-ssl.sh
```

Recrear nginx con puerto 443 y webroot ACME:

```bash
cd infra/docker
docker compose -f docker-compose.staging.yml up -d nginx
```

---

## Paso 3 — Emitir certificado

No requiere `sudo` — Certbot corre en Docker y guarda certs en `infra/docker/certbot/conf/`.

```bash
cd ~/careflow-platform
export CERTBOT_EMAIL="tu-email@ejemplo.com"
./scripts/setup-ssl.sh
```

El script:
1. Verifica que DNS apunta al VPS
2. Pide certificado con Certbot (webroot)
3. Activa `nginx/default.ssl.conf` (redirect HTTP → HTTPS)
4. Reinicia nginx

---

## Paso 4 — Probar

- https://app.careflowhq.org
- https://careflowhq.org
- https://www.careflowhq.org

Login, registro y API `/api/*` deben funcionar con candado verde.

---

## Renovación automática

Certbot renueva certs ~90 días. Cron sugerido (usuario **deploy**, sin sudo):

```bash
crontab -e
```

Añadir:

```
0 3 * * * /home/deploy/careflow-platform/scripts/renew-ssl.sh >> /home/deploy/careflow-ssl-renew.log 2>&1
```

---

## Troubleshooting

### DNS no resuelve
Espera propagación o revisa registros A en el registrador.

### certbot: connection refused / challenge failed
- Firewall Hetzner debe tener **80** y **443** abiertos
- Nginx debe estar corriendo: `docker compose -f docker-compose.staging.yml ps nginx`

### nginx no arranca tras SSL
Certificados faltantes. Vuelve a HTTP temporal:

```bash
cd ~/careflow-platform/infra/docker
git checkout nginx/default.conf   # versión HTTP bootstrap
docker compose -f docker-compose.staging.yml up -d nginx
./scripts/setup-ssl.sh
```

### Sigue funcionando la IP
http://178.105.118.30 sigue en HTTP (sin certificado para IP). Usa el dominio para HTTPS.

---

## Archivos

| Archivo | Rol |
|---------|-----|
| `infra/docker/nginx/default.conf` | HTTP + ACME (bootstrap) |
| `infra/docker/nginx/default.ssl.conf` | Plantilla HTTPS final |
| `infra/docker/certbot/www/` | Webroot desafío ACME |
| `scripts/setup-ssl.sh` | Instalación inicial |
| `scripts/renew-ssl.sh` | Renovación |
