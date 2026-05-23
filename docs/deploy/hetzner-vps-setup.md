# Hetzner VPS — provisión y bootstrap

Documentación de la configuración realizada para el servidor **careflow-staging** en Hetzner Cloud.

## Servidor actual

| Campo | Valor |
|-------|-------|
| **Nombre** | `careflow-staging` |
| **Proveedor** | Hetzner Cloud |
| **Plan** | CPX32 — 4 vCPU AMD, 8 GB RAM, 160 GB SSD |
| **Ubicación** | Falkenstein (`fsn1`) / eu-central |
| **SO** | Ubuntu 24.04 LTS |
| **IP pública IPv4** | `178.105.118.30` |
| **IPv6** | Habilitada (gratis) |
| **Coste aprox.** | ~€13.99/mes (servidor) + ~€0.58/mes (IPv4) |

### Por qué CPX32 (y no CPX22)

CareFlow en staging ejecuta 6 microservicios Java, Postgres, RabbitMQ, Next.js y Nginx. Con **4 GB** (CPX22) la RAM queda justa. **8 GB** da margen con límites JVM de 384 MB por servicio.

Alternativa más económica: **CX33 Intel** (8 GB, ~€6.49/mes) suficiente para staging/demo.

## Provisión en Hetzner (checklist)

### 1. Tipo de servidor

- Arquitectura: **x86 AMD** → CPX32
- Ubicación: la disponible (ej. Falkenstein / Nuremberg)

### 2. Imagen

- **OS Images → Ubuntu 24.04 LTS**
- Alternativa válida: **Apps → Docker CE** (Docker preinstalado; nosotros instalamos Docker manualmente en Ubuntu limpio)

### 3. Red

- **Public IPv4**: sí (requerido para dominio y acceso desde Perú)
- **Public IPv6**: sí (opcional, gratis)
- **Private networks**: no (un solo servidor)

### 4. SSH keys

Añadir clave pública **antes** de crear el servidor.

En Windows (si ya existe clave):

```powershell
cat $env:USERPROFILE\.ssh\id_ed25519.pub
```

Pegar la línea completa en Hetzner → **+ Add SSH key**.

> Si pregunta `Overwrite (y/n)?` al generar nueva clave, responder **`n`** y usar la existente.

### 5. Firewall Hetzner

Crear firewall `careflow-staging-fw` con reglas **inbound**:

| Regla | Protocolo | Puerto | Origen |
|-------|-----------|--------|--------|
| SSH | TCP | 22 | Any IPv4 + IPv6 |
| HTTP | TCP | 80 | Any IPv4 + IPv6 |
| HTTPS | TCP | 443 | Any IPv4 + IPv6 |
| Ping (opcional) | ICMP | — | Any |

**Outbound**: sin reglas (todo permitido).

Aplicar el firewall al servidor `careflow-staging`.

> No abrir 8080–8085, 5433, 5672, 15672 al exterior. Solo Nginx (:80/:443) expuesto.

### 6. Opciones omitidas

| Opción | Decisión |
|--------|----------|
| Volumes extra | No necesario (160 GB incluidos) |
| Backups Hetzner | Off al inicio (+20% costo); activar con datos reales |
| Placement groups | No (un solo nodo) |
| Labels | Opcional: `project=careflow`, `env=staging` |
| Cloud config | Vacío |

## Bootstrap del servidor

Ejecutado como **root** tras crear el VPS.

### Actualizar sistema e instalar Docker

```bash
apt update && apt upgrade -y
curl -fsSL https://get.docker.com | sh
systemctl enable docker
docker --version
docker compose version
```

Versiones verificadas en el servidor:

- Docker 29.x
- Docker Compose v5.x

### Utilidades y firewall local (UFW)

```bash
apt install -y git curl ufw
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable
ufw status
```

UFW complementa el firewall de Hetzner (defensa en profundidad).

### Usuario `deploy`

```bash
adduser deploy
usermod -aG docker deploy
mkdir -p /home/deploy/.ssh
cp ~/.ssh/authorized_keys /home/deploy/.ssh/
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys
```

Conectar como deploy:

```bash
ssh deploy@178.105.118.30
docker ps   # debe funcionar sin sudo
```

Si `permission denied` en Docker:

```bash
# como root
usermod -aG docker deploy
# cerrar sesión deploy y volver a entrar
```

## Acceso SSH desde Windows

```powershell
ssh deploy@178.105.118.30
```

Usar **`deploy`** para operación diaria; reservar `root` para tareas administrativas puntuales.

## DNS (cuando haya dominio)

| Registro | Tipo | Valor |
|----------|------|-------|
| `app` | A | `178.105.118.30` |
| `api` | A | `178.105.118.30` |

Hoy staging responde en la IP directa vía Nginx (ruta única `/` + `/api`).

## Próximo paso

Deploy de la aplicación: [staging-vps.md](./staging-vps.md).
