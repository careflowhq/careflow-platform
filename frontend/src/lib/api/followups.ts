import { api } from "@/lib/api/client";
import type { FollowUp } from "@/types";

export async function listFollowUps() {
  const { data } = await api.get<FollowUp[]>("/followups");
  return data;
}

export async function listPendingFollowUps() {
  const { data } = await api.get<FollowUp[]>("/followups/pending");
  return data;
}

export async function createFollowUp(payload: {
  patientId: string;
  type: string;
  scheduledDate: string;
  notes?: string;
}) {
  const { data } = await api.post<FollowUp>("/followups", payload);
  return data;
}

export async function completeFollowUp(id: string, notes?: string) {
  const { data } = await api.patch<FollowUp>(`/followups/${id}/complete`, notes ? { notes } : {});
  return data;
}

export async function cancelFollowUp(id: string) {
  await api.delete(`/followups/${id}`);
}
