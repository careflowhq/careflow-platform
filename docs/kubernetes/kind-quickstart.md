# Kind — inicio rápido (Sprint 3.0 + 3.1)

Guía para levantar **infraestructura CareFlow** en Kubernetes local con [Kind](https://kind.sigs.k8s.io/).

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
3. Despliega namespace `careflow`
4. Despliega **Postgres** (5 bases vía init script) y **RabbitMQ**
5. Espera pods `Ready`

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

Configurados en `infra/kind/careflow-kind.yaml` para Ingress futuro (Sprint 3.4):

| Host | Uso futuro |
|------|------------|
| `8088` | HTTP (Ingress) |
| `8443` | HTTPS (Ingress) |

En Fase 3.1 no hay Ingress; Postgres y RabbitMQ son **solo internos** al cluster.

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

Verifica con port-forward:

```powershell
kubectl port-forward -n careflow svc/api-gateway 8080:8080
curl http://localhost:8080/actuator/health
```

## Siguiente fase (3.4)
