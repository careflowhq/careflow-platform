import { api } from "@/lib/api/client";
import type { Patient, PatientStatus } from "@/types";

export async function listPatients() {
  const { data } = await api.get<Patient[]>("/patients");
  return data;
}

export async function createPatient(payload: {
  fullName: string;
  phoneNumber: string;
  diagnosis?: string;
  status?: PatientStatus;
}) {
  const { data } = await api.post<Patient>("/patients", {
    ...payload,
    status: payload.status ?? "ACTIVE",
  });
  return data;
}

export async function updatePatient(
  id: string,
  payload: {
    fullName: string;
    phoneNumber: string;
    diagnosis?: string;
    status: PatientStatus;
  }
) {
  const { data } = await api.put<Patient>(`/patients/${id}`, payload);
  return data;
}

export async function deletePatient(id: string) {
  await api.delete(`/patients/${id}`);
}
