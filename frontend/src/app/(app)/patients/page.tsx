"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { createPatient, deletePatient, listPatients, updatePatient } from "@/lib/api/patients";
import { getErrorMessage } from "@/lib/api/client";
import type { Patient, PatientStatus } from "@/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { formatDate } from "@/lib/utils";
import { labelPatientStatus, patientStatusLabels } from "@/lib/labels";

const schema = z.object({
  fullName: z.string().min(1, "Nombre requerido"),
  phoneNumber: z.string().min(1, "Teléfono requerido"),
  diagnosis: z.string().optional(),
  status: z.enum(["ACTIVE", "AT_RISK", "INACTIVE"]),
});

type FormData = z.infer<typeof schema>;

export default function PatientsPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Patient | null>(null);
  const [error, setError] = useState<string | null>(null);

  const patientsQuery = useQuery({ queryKey: ["patients"], queryFn: listPatients });

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: "", phoneNumber: "", diagnosis: "", status: "ACTIVE" },
  });

  const saveMutation = useMutation({
    mutationFn: async (data: FormData) => {
      if (editing) {
        return updatePatient(editing.id, data);
      }
      return createPatient(data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["patients"] });
      form.reset({ fullName: "", phoneNumber: "", diagnosis: "", status: "ACTIVE" });
      setEditing(null);
      setError(null);
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: deletePatient,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const startEdit = (patient: Patient) => {
    setEditing(patient);
    form.reset({
      fullName: patient.fullName,
      phoneNumber: patient.phoneNumber,
      diagnosis: patient.diagnosis ?? "",
      status: patient.status,
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Pacientes</h1>
        <p className="text-slate-600">Gestión de pacientes del consultorio</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{editing ? "Editar paciente" : "Nuevo paciente"}</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={form.handleSubmit((data) => saveMutation.mutate(data))}
            className="grid gap-4 md:grid-cols-2"
          >
            <div>
              <Label>Nombre completo</Label>
              <Input {...form.register("fullName")} />
            </div>
            <div>
              <Label>Teléfono</Label>
              <Input {...form.register("phoneNumber")} />
            </div>
            <div>
              <Label>Diagnóstico</Label>
              <Input {...form.register("diagnosis")} />
            </div>
            <div>
              <Label>Estado</Label>
              <Select {...form.register("status")}>
                {(Object.entries(patientStatusLabels) as [PatientStatus, string][]).map(
                  ([value, text]) => (
                    <option key={value} value={value}>
                      {text}
                    </option>
                  )
                )}
              </Select>
            </div>
            <div className="flex gap-2 md:col-span-2">
              <Button type="submit" disabled={saveMutation.isPending}>
                {editing ? "Guardar cambios" : "Crear paciente"}
              </Button>
              {editing && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setEditing(null);
                    form.reset({ fullName: "", phoneNumber: "", diagnosis: "", status: "ACTIVE" });
                  }}
                >
                  Cancelar
                </Button>
              )}
            </div>
          </form>
          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Listado</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="border-b text-slate-500">
              <tr>
                <th className="py-2 pr-4">Nombre</th>
                <th className="py-2 pr-4">Teléfono</th>
                <th className="py-2 pr-4">Estado</th>
                <th className="py-2 pr-4">Creado</th>
                <th className="py-2">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {(patientsQuery.data ?? []).map((patient) => (
                <tr key={patient.id} className="border-b border-slate-100">
                  <td className="py-3 pr-4 font-medium">{patient.fullName}</td>
                  <td className="py-3 pr-4">{patient.phoneNumber}</td>
                  <td className="py-3 pr-4">
                    <Badge label={patient.status} display={labelPatientStatus(patient.status)} />
                  </td>
                  <td className="py-3 pr-4 text-slate-500">{formatDate(patient.createdAt)}</td>
                  <td className="py-3">
                    <div className="flex gap-2">
                      <Button variant="outline" onClick={() => startEdit(patient)}>
                        Editar
                      </Button>
                      <Button
                        variant="destructive"
                        onClick={() => deleteMutation.mutate(patient.id)}
                        disabled={deleteMutation.isPending}
                      >
                        Eliminar
                      </Button>
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
