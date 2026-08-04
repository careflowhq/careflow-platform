# Migraciones de base de datos — Flyway

CareFlow usa **Flyway** para versionar el esquema de cada microservicio con base de datos propia (database-per-service).

## Servicios con Flyway

| Servicio | Base de datos | Migraciones |
|----------|---------------|-------------|
| auth-service | `auth_db` | `backend/auth-service/src/main/resources/db/migration/` |
| clinic-service | `clinic_db` | `backend/clinic-service/src/main/resources/db/migration/` |
| patient-service | `patient_db` | `backend/patient-service/src/main/resources/db/migration/` |
| followup-service | `followup_db` | `backend/followup-service/src/main/resources/db/migration/` |
| notification-service | `notification_db` | `backend/notification-service/src/main/resources/db/migration/` |

## Configuración Spring Boot

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Hibernate ya no modifica el esquema
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
```

| Propiedad | Propósito |
|-----------|-----------|
| `ddl-auto: validate` | Comprueba que entidades JPA coinciden con el esquema; no crea tablas |
| `baseline-on-migrate` | Bases **existentes** (creadas antes con `ddl-auto: update`) se marcan en v1 sin re-ejecutar V1 |
| `baseline-version: 1` | V1 queda considerada aplicada al hacer baseline |

## Convención de archivos

```
V{version}__{descripcion}.sql
```

Ejemplos:

```
V1__baseline.sql          # Esquema inicial
V2__add_user_phone.sql    # Cambio futuro
```

Reglas:

- Versiones enteras secuenciales (1, 2, 3…)
- Descripción en snake_case
- **Nunca modificar** una migración ya aplicada en un entorno
- Siempre crear un nuevo `V2`, `V3`, etc.

## Flujos por entorno

### Base de datos nueva (volumen Docker vacío)

1. Flyway ejecuta `V1__baseline.sql`
2. Hibernate valida entidades contra tablas creadas

### Base de datos existente (staging/Hetzner con tablas de Hibernate)

1. Flyway detecta esquema no vacío sin historial
2. `baseline-on-migrate` registra versión 1 sin ejecutar V1
3. Hibernate valida; datos existentes se conservan
4. Migraciones `V2+` se aplican normalmente

## Desarrollo local

### Primera vez con Flyway (volúmenes antiguos de `ddl-auto`)

Opción A — **Conservar datos** (recomendado si tienes datos de prueba):

```powershell
cd infra\docker
docker compose -f docker-compose.staging.yml up -d --build auth-service
# Flyway hace baseline automático
```

Opción B — **Empezar desde cero**:

```powershell
docker compose -f docker-compose.staging.yml down -v
docker compose -f docker-compose.staging.yml up -d --build
# Flyway ejecuta V1 en bases vacías
```

### Infra local (`docker-compose.yml` + apps en host)

Tras levantar Postgres:

```powershell
cd backend/auth-service
mvn spring-boot:run
# Flyway migra auth_db al arrancar
```

Repetir por servicio, o usar el stack staging containerizado.

## Verificar estado Flyway

```sql
-- Conectar a la base del servicio
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Añadir un cambio de esquema (ejemplo)

1. Editar entidad JPA en el servicio afectado
2. Crear `V2__add_column_x.sql` en ese servicio únicamente
3. Probar localmente
4. Desplegar — Flyway aplica V2 al arrancar

```sql
-- V2__add_user_phone.sql (ejemplo ficticio)
ALTER TABLE users ADD COLUMN phone VARCHAR(32);
```

## Sprint 2 — alcance

- Migración baseline `V1__baseline.sql` por servicio
- Sustitución de `ddl-auto: update` por `validate` + Flyway
- Compatibilidad con bases existentes vía `baseline-on-migrate`

## Próximos sprints

| Sprint | Mejora |
|--------|--------|
| 9 | Extraer convenciones comunes a `shared-libs` |
| 10 | Flyway en pipeline CI/CD (validar migraciones antes de deploy) |
