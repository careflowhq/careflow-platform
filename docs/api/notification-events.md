# Notification Events (Phase 1 contract)

Contrato de eventos async vía **RabbitMQ** entre `followup-service` / `auth-service` (productores) y `notification-service` (consumidor).

> Estado: **draft** — implementación en Fase 1. Infra RabbitMQ ya disponible en `infra/docker/`.

## Exchange y routing

| Propiedad | Valor |
|-----------|-------|
| Exchange | `careflow.events` (topic) |
| Queue consumidor | `notification-service.queue` |

## Eventos MVP (demo)

### `followup.scheduled`

Publicado cuando se crea un seguimiento (`POST /followups`).

```json
{
  "eventType": "followup.scheduled",
  "eventId": "uuid",
  "occurredAt": "2026-05-19T15:00:00Z",
  "clinicId": "uuid",
  "patientId": "uuid",
  "followUpId": "uuid",
  "payload": {
    "type": "POST_CONSULTATION",
    "scheduledDate": "2026-05-20T14:00:00Z",
    "patientPhone": "+51999999999",
    "patientName": "María López"
  }
}
```

**Acción notification-service:** crear registro + mensaje de confirmación/programación. Canal demo: `WHATSAPP_LINK`.

---

### `followup.missed`

Publicado cuando el scheduler marca un seguimiento como `MISSED`.

```json
{
  "eventType": "followup.missed",
  "eventId": "uuid",
  "occurredAt": "2026-05-19T15:00:00Z",
  "clinicId": "uuid",
  "patientId": "uuid",
  "followUpId": "uuid",
  "payload": {
    "type": "POST_CONSULTATION",
    "scheduledDate": "2026-05-18T10:00:00Z",
    "patientPhone": "+51999999999",
    "patientName": "María López"
  }
}
```

**Acción notification-service:** alerta al paciente + registro en historial.

---

### `staff.invited` (fase posterior)

Publicado por `auth-service` al crear invitación. Reemplaza el envío manual del token.

```json
{
  "eventType": "staff.invited",
  "eventId": "uuid",
  "occurredAt": "2026-05-19T15:00:00Z",
  "clinicId": "uuid",
  "payload": {
    "email": "doctor@consultorio.com",
    "fullName": "Dr. Ana",
    "role": "DOCTOR",
    "inviteToken": "uuid",
    "expiresAt": "2026-05-26T15:00:00Z"
  }
}
```

## Canales de entrega (notification-service)

| Canal | MVP demo | Producción |
|-------|----------|------------|
| `WHATSAPP_LINK` | URL `wa.me/{phone}?text=...` | — |
| `WHATSAPP_API` | — | Meta Cloud API / Twilio |
| `EMAIL` | — | Resend / SMTP |
| `LOG` | Solo desarrollo | — |

## API REST (notification-service, vía gateway)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/notifications` | Historial por `clinicId` (JWT) |
| `POST` | `/api/notifications/{id}/send` | Reenvío manual (demo) |

## Idempotencia

- Clave: `eventId` único por evento
- `notification-service` ignora duplicados con mismo `eventId`

## Relacionado

- [ADR 0003](../adr/0003-rabbitmq-for-notifications.md)
- [domain-boundaries.md](../architecture/domain-boundaries.md)
- [followups.md](../business-rules/followups.md)
