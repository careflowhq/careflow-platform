export type UserRole = "PLATFORM_ADMIN" | "CLINIC_ADMIN" | "DOCTOR" | "ASSISTANT";

export type PatientStatus = "ACTIVE" | "AT_RISK" | "INACTIVE";

export type FollowUpStatus = "PENDING" | "COMPLETED" | "MISSED" | "CANCELLED";

export interface LoginResponse {
  token: string;
}

export interface Patient {
  id: string;
  clinicId: string;
  assignedDoctorId: string | null;
  fullName: string;
  phoneNumber: string;
  diagnosis: string | null;
  status: PatientStatus;
  createdAt: string;
}

export interface FollowUp {
  id: string;
  clinicId: string;
  patientId: string;
  doctorId: string | null;
  type: string;
  scheduledDate: string;
  status: FollowUpStatus;
  notes: string | null;
  createdBy: string;
  createdAt: string;
}

export interface InviteResponse {
  token: string;
  email: string;
  fullName: string;
  role: UserRole;
  clinicId: string;
  expiresAt: string;
}

export interface JwtClaims {
  sub: string;
  clinicId: string;
  role: UserRole;
  exp: number;
}

export interface ApiProblem {
  detail?: string;
  title?: string;
}
