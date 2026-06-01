# SSL — careflowhq.org

HTTPS con **Let's Encrypt** (Certbot) en el VPS Hetzner.

## Estado actual (staging)

**SSL activo** desde mayo 2026. Los tres hosts sirven la misma aplicación (Nginx → frontend Next.js + `/api` → API Gateway).

| URL | Uso recomendado |
|-----|-----------------|
| **https://app.careflowhq.org** | URL principal para demo, login y registro |
| **https://careflowhq.org** | Dominio raíz — misma app (redirect interno no aplica; contenido idéntico) |
| **https://www.careflowhq.org** | Alias de la raíz |

HTTP (`http://…`) redirige a HTTPS. La IP **http://178.105.118.30** sigue disponible solo en HTTP (Let's Encrypt no emite certificados para IP).

Servidor: `178.105.118.30` · Usuario deploy: `deploy@178.105.118.30`

---

## Dominios en el certificado

| Host | Incluido en cert |
|------|------------------|
| `app.careflowhq.org` | Sí (CN principal) |
| `careflowhq.org` | Sí |
| `www.careflowhq.org` | Sí |

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
2. Pide certificado con Certbot (webroot, imagen Docker)
3. Ajusta permisos de lectura para Nginx (archivos creados como root por Certbot)
4. Copia `nginx/default.ssl.conf` → `default.conf` (redirect HTTP → HTTPS)
5. Reinicia nginx y comprueba HTTPS localmente

---

## Paso 4 — Probar

Abre en el navegador:

- https://app.careflowhq.org/register
- https://careflowhq.org/login
- https://www.careflowhq.org

Comprueba:

- Candado verde (certificado Let's Encrypt válido)
- Login, registro y rutas `/api/*` responden correctamente
- Misma UI en `app.` y en la raíz (comportamiento esperado en staging)

---

## Renovación automática

Los certificados Let's Encrypt expiran a los ~90 días. La renovación corre **automáticamente** vía cron del usuario `deploy` (sin sudo).

### Instalación (una vez)

Tras el setup SSL, o en cualquier momento:

```bash
cd ~/careflow-platform
chmod +x scripts/renew-ssl.sh scripts/install-ssl-renew-cron.sh
./scripts/install-ssl-renew-cron.sh
```

`setup-ssl.sh` ejecuta este paso al final. Entrada cron:

```
0 3 * * * /home/deploy/careflow-platform/scripts/renew-ssl.sh >> /home/deploy/careflow-ssl-renew.log 2>&1
```

- **Frecuencia:** diaria a las 03:00 UTC
- **Log:** `~/careflow-ssl-renew.log`
- Certbot solo renueva cuando faltan ≤30 días para expirar

### Verificar

```bash
crontab -l | grep careflow-ssl-renew
tail -20 ~/careflow-ssl-renew.log
```

Prueba manual (sin esperar al cron):

```bash
./scripts/renew-ssl.sh
```

---

## Troubleshooting

### DNS no resuelve
Espera propagación o revisa registros A en el registrador.

### certbot: connection refused / challenge failed
- Firewall Hetzner debe tener **80** y **443** abiertos
- Nginx debe estar corriendo: `docker compose -f docker-compose.staging.yml ps nginx`

### nginx no arranca tras SSL
Certificados faltantes o permisos incorrectos. Revisa logs:

```bash
cd ~/careflow-platform/infra/docker
docker compose -f docker-compose.staging.yml logs nginx --tail=30
```

Si faltan certs, vuelve a HTTP bootstrap y repite setup:

```bash
cd ~/careflow-platform/infra/docker
git checkout nginx/default.conf   # versión HTTP bootstrap
docker compose -f docker-compose.staging.yml up -d nginx
cd ~/careflow-platform
./scripts/setup-ssl.sh
```

### HTTPS: ERR_CONNECTION_REFUSED o SSL_ERROR_SYSCALL
Suele ser **permisos**: Certbot (Docker) crea archivos como root y el contenedor nginx (usuario no root) no puede leer `privkey.pem`.

**Fix sin sudo** (usuario `deploy`):

```bash
cd ~/careflow-platform/infra/docker
docker run --rm -v "$(pwd)/certbot/conf:/etc/letsencrypt" alpine sh -c \
  "chmod -R a+rX /etc/letsencrypt/live /etc/letsencrypt/archive && chmod a+r /etc/letsencrypt/archive/app.careflowhq.org/privkey1.pem"
docker compose -f docker-compose.staging.yml restart nginx
```

Verifica desde el VPS:

```bash
curl -fsSk https://127.0.0.1 -o /dev/null -H "Host: app.careflowhq.org" && echo OK
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
| `scripts/renew-ssl.sh` | Renovación (ejecutado por cron) |
| `scripts/install-ssl-renew-cron.sh` | Instala cron idempotente |
