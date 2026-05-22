import { api } from "@/lib/api/client";

export type NotificationChannel = "WHATSAPP_LINK" | "LOG";
export type NotificationStatus = "PENDING" | "READY" | "SENT" | "FAILED";

export interface Notification {
  id: string;
  clinicId: string;
  patientId: string | null;
  followUpId: string | null;
  eventType: string;
  channel: NotificationChannel;
  status: NotificationStatus;
  recipientName: string | null;
  recipientPhone: string | null;
  message: string;
  deliveryUrl: string | null;
  createdAt: string;
  sentAt: string | null;
}

export async function listNotifications() {
  const { data } = await api.get<Notification[]>("/notifications");
  return data;
}

export async function markNotificationSent(id: string) {
  const { data } = await api.post<Notification>(`/notifications/${id}/send`);
  return data;
}
