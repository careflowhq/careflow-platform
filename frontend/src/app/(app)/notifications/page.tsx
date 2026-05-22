"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink } from "lucide-react";
import { listNotifications, markNotificationSent } from "@/lib/api/notifications";
import { getErrorMessage } from "@/lib/api/client";
import { labelNotificationEvent, labelNotificationStatus } from "@/lib/labels";
import { formatDate } from "@/lib/utils";
import { formatPhoneDisplay } from "@/lib/phone";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: listNotifications,
  });

  const sendMutation = useMutation({
    mutationFn: markNotificationSent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
  });

  const items = notificationsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Notificaciones</h1>
        <p className="text-slate-600">
          Mensajes generados para pacientes. En la demo se envían por WhatsApp con un clic.
        </p>
      </div>

      {notificationsQuery.error && (
        <p className="text-sm text-red-600">{getErrorMessage(notificationsQuery.error)}</p>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Historial</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          {items.length === 0 ? (
            <p className="text-sm text-slate-500">
              Aún no hay notificaciones. Creá un seguimiento para generar el primer mensaje al
              paciente.
            </p>
          ) : (
            <table className="w-full min-w-[880px] text-left text-sm">
              <thead className="border-b text-slate-500">
                <tr>
                  <th className="py-2 pr-4">Paciente</th>
                  <th className="py-2 pr-4">Tipo</th>
                  <th className="py-2 pr-4">Mensaje</th>
                  <th className="py-2 pr-4">Estado</th>
                  <th className="py-2 pr-4">Creada</th>
                  <th className="py-2">Acción</th>
                </tr>
              </thead>
              <tbody>
                {items.map((n) => (
                  <tr key={n.id} className="border-b border-slate-100 align-top">
                    <td className="py-3 pr-4">
                      <p className="font-medium">{n.recipientName ?? "—"}</p>
                      <p className="text-slate-500">
                        {n.recipientPhone ? formatPhoneDisplay(n.recipientPhone) : "Sin teléfono"}
                      </p>
                    </td>
                    <td className="py-3 pr-4">{labelNotificationEvent(n.eventType)}</td>
                    <td className="max-w-xs py-3 pr-4 text-slate-600">{n.message}</td>
                    <td className="py-3 pr-4">
                      <Badge label={n.status} display={labelNotificationStatus(n.status)} />
                    </td>
                    <td className="py-3 pr-4 text-slate-500">{formatDate(n.createdAt)}</td>
                    <td className="py-3">
                      {n.deliveryUrl && n.status !== "SENT" ? (
                        <div className="flex flex-col gap-2">
                          <a href={n.deliveryUrl} target="_blank" rel="noopener noreferrer">
                            <Button variant="outline" className="w-full">
                              <ExternalLink className="mr-2 h-4 w-4" />
                              Abrir WhatsApp
                            </Button>
                          </a>
                          <Button
                            variant="default"
                            disabled={sendMutation.isPending}
                            onClick={() => sendMutation.mutate(n.id)}
                          >
                            Marcar enviada
                          </Button>
                        </div>
                      ) : n.status === "SENT" ? (
                        <span className="text-slate-500">
                          Enviada{n.sentAt ? ` · ${formatDate(n.sentAt)}` : ""}
                        </span>
                      ) : (
                        <span className="text-slate-400">Sin enlace</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
