"use client";

import { Suspense, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  cancelFollowUp,
  completeFollowUp,
  createFollowUp,
  listFollowUps,
} from "@/lib/api/followups";
import { listPatients } from "@/lib/api/patients";
import { getErrorMessage } from "@/lib/api/client";
import type { FollowUpStatus } from "@/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  combineDateAndTime,
  defaultScheduleDate,
  defaultScheduleTime,
  formatDate,
} from "@/lib/utils";
import { followUpTypeOptions, followUpTypeLabel, labelFollowUpStatus } from "@/lib/labels";

const schema = z.object({
  patientId: z.string().min(1, "Selecciona un paciente"),
  type: z.string().min(1, "Tipo requerido"),
  scheduledDate: z.string().min(1, "Fecha requerida"),
  scheduledTime: z.string().min(1, "Hora requerida"),
  notes: z.string().optional(),
});

type FormData = z.infer<typeof schema>;
type Filter = "ALL" | FollowUpStatus;

const validFilters: Filter[] = ["ALL", "PENDING", "MISSED", "COMPLETED", "CANCELLED"];

function parseFilter(value: string | null): Filter {
  if (value && validFilters.includes(value as Filter)) {
    return value as Filter;
  }
  return "ALL";
}

function FollowUpsContent() {
  const searchParams = useSearchParams();
  const initialFilter = parseFilter(searchParams.get("estado"));
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<Filter>(initialFilter);
  const [error, setError] = useState<string | null>(null);

  const followUpsQuery = useQuery({ queryKey: ["followups"], queryFn: listFollowUps });
  const patientsQuery = useQuery({ queryKey: ["patients"], queryFn: listPatients });

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      patientId: "",
      type: "POST_CONSULTATION",
      scheduledDate: defaultScheduleDate(),
      scheduledTime: defaultScheduleTime,
      notes: "",
    },
  });

  const patientMap = useMemo(
    () => Object.fromEntries((patientsQuery.data ?? []).map((p) => [p.id, p.fullName])),
    [patientsQuery.data]
  );

  const filtered = useMemo(() => {
    const items = followUpsQuery.data ?? [];
    return filter === "ALL" ? items : items.filter((f) => f.status === filter);
  }, [followUpsQuery.data, filter]);

  const createMutation = useMutation({
    mutationFn: (data: FormData) =>
      createFollowUp({
        patientId: data.patientId,
        type: data.type,
        scheduledDate: combineDateAndTime(data.scheduledDate, data.scheduledTime),
        notes: data.notes,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["followups"] });
      form.reset({
        patientId: "",
        type: "POST_CONSULTATION",
        scheduledDate: defaultScheduleDate(),
        scheduledTime: defaultScheduleTime,
        notes: "",
      });
      setError(null);
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => completeFollowUp(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["followups"] }),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const cancelMutation = useMutation({
    mutationFn: cancelFollowUp,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["followups"] }),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const filters: { key: Filter; label: string }[] = [
    { key: "ALL", label: "Todos" },
    { key: "PENDING", label: "Pendientes" },
    { key: "MISSED", label: "Vencidos" },
    { key: "COMPLETED", label: "Completados" },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Seguimientos</h1>
        <p className="text-slate-600">Seguimientos programados del consultorio</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Nuevo seguimiento</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={form.handleSubmit((data) => createMutation.mutate(data))}
            className="grid gap-4 md:grid-cols-2"
          >
            <div>
              <Label>Paciente</Label>
              <Select {...form.register("patientId")}>
                <option value="">Seleccionar…</option>
                {(patientsQuery.data ?? []).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.fullName}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label>Tipo de seguimiento</Label>
              <Select {...form.register("type")}>
                {followUpTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>
            <div className="md:col-span-2">
              <p className="mb-2 text-sm font-medium text-slate-700">Cuándo programar</p>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <Label htmlFor="scheduledDate">Fecha</Label>
                  <Input id="scheduledDate" type="date" {...form.register("scheduledDate")} />
                </div>
                <div>
                  <Label htmlFor="scheduledTime">Hora</Label>
                  <Input
                    id="scheduledTime"
                    type="time"
                    step={900}
                    {...form.register("scheduledTime")}
                  />
                </div>
              </div>
              <p className="mt-1.5 text-xs text-slate-500">Horario local del consultorio · intervalos de 15 min</p>
            </div>
            <div className="md:col-span-2">
              <Label>Notas</Label>
              <Textarea rows={2} {...form.register("notes")} />
            </div>
            <div>
              <Button type="submit" disabled={createMutation.isPending}>
                Crear seguimiento
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        {filters.map((f) => (
          <Button
            key={f.key}
            variant={filter === f.key ? "default" : "outline"}
            onClick={() => setFilter(f.key)}
          >
            {f.label}
          </Button>
        ))}
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <Card>
        <CardContent className="overflow-x-auto pt-6">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b text-slate-500">
              <tr>
                <th className="py-2 pr-4">Paciente</th>
                <th className="py-2 pr-4">Tipo de seguimiento</th>
                <th className="py-2 pr-4">Programado</th>
                <th className="py-2 pr-4">Estado</th>
                <th className="py-2">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((f) => (
                <tr key={f.id} className="border-b border-slate-100">
                  <td className="py-3 pr-4 font-medium">{patientMap[f.patientId] ?? f.patientId}</td>
                  <td className="py-3 pr-4">{followUpTypeLabel(f.type)}</td>
                  <td className="py-3 pr-4">{formatDate(f.scheduledDate)}</td>
                  <td className="py-3 pr-4">
                    <Badge label={f.status} display={labelFollowUpStatus(f.status)} />
                  </td>
                  <td className="py-3">
                    <div className="flex gap-2">
                      {f.status === "PENDING" && (
                        <>
                          <Button variant="outline" onClick={() => completeMutation.mutate(f.id)}>
                            Completar
                          </Button>
                          <Button variant="destructive" onClick={() => cancelMutation.mutate(f.id)}>
                            Cancelar
                          </Button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}

export default function FollowUpsPage() {
  return (
    <Suspense fallback={<div className="text-slate-500">Cargando seguimientos…</div>}>
      <FollowUpsContent />
    </Suspense>
  );
}
