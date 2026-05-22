import { api } from "@/lib/api/client";
import type { InviteResponse, LoginResponse } from "@/types";

export async function login(email: string, password: string) {
  const { data } = await api.post<LoginResponse>("/auth/login", { email, password });
  return data;
}

export async function registerClinic(payload: {
  fullName: string;
  email: string;
  password: string;
  clinicName: string;
  country: string;
  timezone: string;
}) {
  await api.post("/auth/register", {
    ...payload,
    role: "CLINIC_ADMIN",
  });
}

export async function registerInvite(token: string, password: string) {
  await api.post("/auth/register-invite", { token, password });
}

export async function inviteStaff(payload: {
  fullName: string;
  email: string;
  role: "DOCTOR" | "ASSISTANT";
}) {
  const { data } = await api.post<InviteResponse>("/auth/invite", payload);
  return data;
}
