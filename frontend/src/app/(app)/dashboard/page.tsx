"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { AlertTriangle, CalendarClock } from "lucide-react";
import { listFollowUps } from "@/lib/api/followups";
import { listPatients } from "@/lib/api/patients";
import { formatDate } from "@/lib/utils";
import { labelFollowUpStatus, followUpTypeLabel } from "@/lib/labels";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function DashboardPage() {
  const patientsQuery = useQuery({ queryKey: ["patients"], queryFn: listPatients });
  const followUpsQuery = useQuery({ queryKey: ["followups"], queryFn: listFollowUps });

  const patients = patientsQuery.data ?? [];
  const followUps = followUpsQuery.data ?? [];
  const pending = useMemo(
    () =>
      followUps
        .filter((f) => f.status === "PENDING")
        .sort((a, b) => new Date(a.scheduledDate).getTime() - new Date(b.scheduledDate).getTime()),
    [followUps]
  );
  const missed = useMemo(
    () =>
      followUps
        .filter((f) => f.status === "MISSED")
        .sort((a, b) => new Date(b.scheduledDate).getTime() - new Date(a.scheduledDate).getTime()),
    [followUps]
  );
  const atRiskPatients = patients.filter((p) => p.status === "AT_RISK");
  const activePatients = patients.filter((p) => p.status === "ACTIVE");

  const patientMap = Object.fromEntries(patients.map((p) => [p.id, p.fullName]));
  const needsAttention = missed.length + atRiskPatients.length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
        <p className="text-slate-600">Resumen de tu consultorio</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard title="Pacientes activos" value={activePatients.length} href="/patients" />
        <MetricCard title="Total pacientes" value={patients.length} href="/patients" />
        <MetricCard title="Seguimientos pendientes" value={pending.length} href="/followups?estado=PENDING" />
        <MetricCard
          title="Seguimientos vencidos"
          value={missed.length}
          highlight={missed.length > 0}
          href="/followups?estado=MISSED"
        />
      </div>

      {needsAttention > 0 && (
        <Card className="border-amber-200 bg-amber-50/50">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-amber-900">
              <AlertTriangle className="h-5 w-5" />
              Requiere atención ({needsAttention})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {missed.length > 0 && (
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-sm font-medium text-amber-900">Seguimientos vencidos</p>
                  <Link href="/followups?estado=MISSED">
                    <Button variant="outline" className="px-3 py-1.5 text-xs">
                      Ver todos
                    </Button>
                  </Link>
                </div>
                <ul className="divide-y divide-amber-100 rounded-lg border border-amber-100 bg-white">
                  {missed.slice(0, 5).map((f) => (
                    <li key={f.id} className="flex items-center justify-between px-4 py-3 text-sm">
                      <div>
                        <p className="font-medium">{patientMap[f.patientId] ?? "Paciente"}</p>
                        <p className="text-slate-500">
                          {followUpTypeLabel(f.type)} · venció {formatDate(f.scheduledDate)}
                        </p>
                      </div>
                      <Badge label={f.status} display={labelFollowUpStatus(f.status)} />
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {atRiskPatients.length > 0 && (
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-sm font-medium text-amber-900">Pacientes en riesgo</p>
                  <Link href="/patients">
                    <Button variant="outline" className="px-3 py-1.5 text-xs">
                      Ver pacientes
                    </Button>
                  </Link>
                </div>
                <ul className="divide-y divide-amber-100 rounded-lg border border-amber-100 bg-white">
                  {atRiskPatients.slice(0, 5).map((p) => (
                    <li key={p.id} className="flex items-center justify-between px-4 py-3 text-sm">
                      <div>
                        <p className="font-medium">{p.fullName}</p>
                        <p className="text-slate-500">{p.phoneNumber}</p>
                      </div>
                      <Badge label={p.status} display="En riesgo" />
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CalendarClock className="h-5 w-5 text-teal-700" />
            Próximos seguimientos
          </CardTitle>
          <Link href="/followups?estado=PENDING">
            <Button variant="outline">Ver todos</Button>
          </Link>
        </CardHeader>
        <CardContent>
          {pending.length === 0 ? (
            <p className="text-sm text-slate-500">No hay seguimientos pendientes programados.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {pending.slice(0, 5).map((f) => (
                <li key={f.id} className="flex items-center justify-between py-3 text-sm">
                  <div>
                    <p className="font-medium">{patientMap[f.patientId] ?? "Paciente"}</p>
                    <p className="text-slate-500">
                      {followUpTypeLabel(f.type)} · {formatDate(f.scheduledDate)}
                    </p>
                  </div>
                  <Badge label={f.status} display={labelFollowUpStatus(f.status)} />
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function MetricCard({
  title,
  value,
  highlight,
  href,
}: {
  title: string;
  value: number;
  highlight?: boolean;
  href?: string;
}) {
  const content = (
    <Card className={href ? "transition hover:border-teal-200 hover:shadow-sm" : undefined}>
      <CardContent className="pt-6">
        <p className="text-sm text-slate-500">{title}</p>
        <p className={`mt-1 text-3xl font-bold ${highlight ? "text-red-600" : "text-slate-900"}`}>{value}</p>
      </CardContent>
    </Card>
  );

  if (href) {
    return <Link href={href}>{content}</Link>;
  }

  return content;
}
