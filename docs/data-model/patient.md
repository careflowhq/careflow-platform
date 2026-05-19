# Patient Entity

## Fields

- id (UUID)
- clinicId
- assignedDoctorId
- fullName
- phoneNumber
- diagnosis
- status
- createdAt

## Status Enum

- ACTIVE
- AT_RISK
- INACTIVE

## Relationships

- belongs to clinic
- assigned to doctor
- has appointments
- has follow-ups
- has alerts