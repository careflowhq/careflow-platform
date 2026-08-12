# Kind — inicio rápido (Sprint 3.0 – 3.4)

Guía para levantar **CareFlow completo** en Kubernetes local con [Kind](https://kind.sigs.k8s.io/).

## Requisitos

| Herramienta | Verificar |
|-------------|-----------|
| Docker Desktop | `docker version` |
| kind v0.32+ | `kind version` |
| kubectl | `kubectl version --client` |
| k9s (opcional) | `k9s version` |

### RAM (WSL2)

Docker Desktop con backend WSL2 usa `.wslconfig` en tu carpeta de usuario:

```ini
# C:\Users\<tu-usuario>\.wslconfig
[wsl2]
memory=6GB
processors=4
swap=2GB
```

Tras editarlo: `wsl --shutdown` y reinicia Docker Desktop.

## Antes de empezar

1. Para el stack Docker Compose staging (libera RAM y puerto 80):

```powershell
cd infra\docker
docker compose -f docker-compose.staging.yml down
```

2. Asegúrate de que Docker Desktop está **Running**.

## Un comando — cluster + infra

Desde la **raíz del repo**:

```powershell
.\scripts\kind-up.ps1
```

Linux / Git Bash:

```bash
chmod +x scripts/kind-*.sh
./scripts/kind-up.sh
```

### Qué hace `kind-up`

1. Crea cluster Kind `careflow-local` (si no existe)
2. Configura contexto kubectl `kind-careflow-local`
3. Instala **ingress-nginx** (controlador Ingress para Kind)
4. Despliega namespace `careflow`
5. Despliega **Postgres** (5 bases vía init script) y **RabbitMQ**
6. Espera pods `Ready`

### Salida esperada

```text
NAME                        READY   STATUS    RESTARTS   AGE
postgres-xxxxxxxxxx-xxxxx   1/1     Running   0          1m
rabbitmq-xxxxxxxxxx-xxxxx   1/1     Running   0          1m
```

## Verificar

```powershell
kubectl get all -n careflow
k9s -n careflow
```

Probar Postgres desde un pod temporal:

```powershell
kubectl run -n careflow psql-test --rm -it --restart=Never `
  --image=postgres:16-alpine `
  --env="PGPASSWORD=careflow-local-k8s-dev" `
  -- psql -h postgres -U careflow -d auth_db -c "\dt"
```

Debería conectar (tablas vacías hasta desplegar apps en Fase 3.2+).

## Credenciales locales (Kind)

Definidas en `infra/kubernetes/base/postgres/secret.yaml`:

| Variable | Valor local |
|----------|-------------|
| `POSTGRES_USER` | `careflow` |
| `POSTGRES_PASSWORD` | `careflow-local-k8s-dev` |

Solo para desarrollo local. **No usar en Hetzner/AKS.**

## Puertos del host

Configurados en `infra/kind/careflow-kind.yaml`:

| Host | Uso |
|------|-----|
| `8088` | HTTP — Ingress (frontend + API) |
| `8443` | HTTPS (futuro) |

Postgres y RabbitMQ son **solo internos** al cluster.

## Despliegue completo (recomendado)

Desde la raíz del repo, con cluster ya creado (`kind-up`):

```powershell
.\scripts\kind-deploy-all.ps1
```

Linux / Git Bash:

```bash
./scripts/kind-deploy-all.sh
```

Abre en el navegador: **http://localhost:8088**

### Rutas Ingress

| Ruta | Destino |
|------|---------|
| `/api/*` | `api-gateway:8080` |
| `/*` | `frontend:3000` |

Mismo criterio que nginx en Docker Compose staging.

### Verificar API vía Ingress

```powershell
curl.exe http://localhost:8088/api/auth/login
# POST con body JSON — ver ejemplos en sección de pruebas 3.3
```

### Verificar frontend

```powershell
curl.exe -I http://localhost:8088/
```

Debe responder `HTTP/1.1 200` (o `307` a login).

## Destruir el cluster

```powershell
.\scripts\kind-down.ps1
```

Elimina el cluster y todos los datos del PVC de Postgres en Kind.

## Troubleshooting

### `Docker is not running`

Abre Docker Desktop y espera a **Running**.

### Pod stuck in `Pending` on Kind

Kind uses a single control-plane node with a taint. CareFlow manifests include tolerations for local development. If you see `FailedScheduling` / untolerated taint, run:

```powershell
kubectl apply -k infra/kubernetes/base
```

### Pod `CrashLoopBackOff`

```powershell
kubectl logs -n careflow deploy/postgres
kubectl describe pod -n careflow -l app.kubernetes.io/name=postgres
```

### Cambiar contraseña Postgres

Edita `infra/kubernetes/base/postgres/secret.yaml` y reaplica:

```powershell
kubectl apply -k infra/kubernetes/base
kubectl rollout restart -n careflow deploy/postgres
```

> Si el PVC ya tiene datos, borrar el cluster (`kind-down`) y volver a crear puede ser más simple en local.

## Siguiente fase (3.2)

Desplegar solo `auth-service` (primer microservicio):

```powershell
.\scripts\kind-deploy-auth.ps1
```

## Fase 3.3 — backend completo

Despliega todos los microservicios backend + `api-gateway`:

```powershell
.\scripts\kind-deploy-apps.ps1
```

Linux / Git Bash:

```bash
./scripts/kind-deploy-apps.sh
```

Orden de arranque (init containers + waits):

1. `auth-service`, `clinic-service`, `patient-service` (Postgres)
2. `followup-service`, `notification-service` (Postgres + RabbitMQ + patient)
3. `api-gateway` (espera health UP de todos los backends)

Secret compartido: `infra/kubernetes/apps/secrets.yaml` (`careflow-app-secrets`).

Verifica con port-forward (alternativa sin Ingress):

```powershell
kubectl port-forward -n careflow svc/api-gateway 8080:8080
curl http://localhost:8080/actuator/health
```

## Fase 3.4 — frontend + Ingress

Incluido en `kind-deploy-all.ps1`:

1. `frontend` (Next.js standalone, puerto 3000)
2. Reglas Ingress en `infra/kubernetes/apps/ingress.yaml`
3. Entrada única: **http://localhost:8088**

Solo frontend + ingress (cluster y backend ya desplegados):

```powershell
.\scripts\kind-install-ingress.ps1
docker compose -f infra/docker/docker-compose.build.yml build frontend
kind load docker-image careflow/frontend:0.0.1-SNAPSHOT --name careflow-local
kubectl apply -k infra/kubernetes/apps
```

## Pruebas funcionales (PowerShell)

Terminal con port-forward **no necesaria** si usas Ingress en `:8088`.

```powershell
# Health del gateway (vía port-forward opcional)
curl.exe http://localhost:8088/

# Login
$loginBody = @{ email = "admin@test.local"; password = "Test1234!" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8088/api/auth/login" `
  -Method POST -ContentType "application/json" -Body $loginBody
```

> `409` en register = email ya existe. `201` sin body = registro OK.
