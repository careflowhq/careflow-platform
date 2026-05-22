import type { FollowUpStatus, PatientStatus, UserRole } from "@/types";

/** Etiquetas en español (LATAM) para enums del backend. */

export const userRoleLabels: Record<UserRole, string> = {
  PLATFORM_ADMIN: "Administrador de plataforma",
  CLINIC_ADMIN: "Administrador de clínica",
  DOCTOR: "Doctor",
  ASSISTANT: "Asistente",
};

/** Roles invitables desde la pantalla de equipo. */
export const staffRoleOptions = [
  { value: "DOCTOR" as const, label: "Doctor" },
  { value: "ASSISTANT" as const, label: "Asistente" },
];

export function labelUserRole(role: UserRole | string | null | undefined): string {
  if (!role) return "";
  return userRoleLabels[role as UserRole] ?? role;
}

export const patientStatusLabels: Record<PatientStatus, string> = {
  ACTIVE: "Activo",
  AT_RISK: "En riesgo",
  INACTIVE: "Inactivo",
};

export const followUpStatusLabels: Record<FollowUpStatus, string> = {
  PENDING: "Pendiente",
  COMPLETED: "Completado",
  MISSED: "Vencido",
  CANCELLED: "Cancelado",
};

/** Tipos de seguimiento — el backend guarda el código en inglés. */
export const followUpTypeOptions = [
  { value: "POST_CONSULTATION", label: "Seguimiento post consulta" },
  { value: "APPOINTMENT_REMINDER", label: "Recordatorio de cita" },
  { value: "MEDICATION_CHECK", label: "Control de medicación" },
  { value: "GENERAL", label: "Seguimiento general" },
] as const;

export function followUpTypeLabel(type: string): string {
  return followUpTypeOptions.find((o) => o.value === type)?.label ?? type;
}

export function labelPatientStatus(status: PatientStatus | string): string {
  return patientStatusLabels[status as PatientStatus] ?? status;
}

export function labelFollowUpStatus(status: FollowUpStatus | string): string {
  return followUpStatusLabels[status as FollowUpStatus] ?? status;
}
