"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { inviteStaff } from "@/lib/api/auth";
import { getErrorMessage } from "@/lib/api/client";
import { useAuthStore } from "@/lib/auth-store";
import { formatDate } from "@/lib/utils";
import { labelUserRole, staffRoleOptions } from "@/lib/labels";
import type { InviteResponse } from "@/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";

const schema = z.object({
  fullName: z.string().min(1, "Nombre requerido"),
  email: z.email("Email inválido"),
  role: z.enum(["DOCTOR", "ASSISTANT"]),
});

type FormData = z.infer<typeof schema>;

export default function TeamPage() {
  const router = useRouter();
  const role = useAuthStore((s) => s.getRole());
  const [inviteResult, setInviteResult] = useState<InviteResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: "", email: "", role: "DOCTOR" },
  });

  const inviteMutation = useMutation({
    mutationFn: inviteStaff,
    onSuccess: (data) => {
      setInviteResult(data);
      setError(null);
      form.reset({ fullName: "", email: "", role: "DOCTOR" });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  if (role !== "CLINIC_ADMIN") {
    return (
      <Card>
        <CardContent className="py-8 text-sm text-slate-600">
          Solo el administrador de la clínica puede invitar miembros del equipo.
          <div className="mt-4">
            <Button variant="outline" onClick={() => router.push("/dashboard")}>
              Volver al dashboard
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Equipo</h1>
        <p className="text-slate-600">Invita doctores y asistentes a tu consultorio</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Invitar miembro</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={form.handleSubmit((data) => inviteMutation.mutate(data))}
            className="grid max-w-xl gap-4"
          >
            <div>
              <Label>Nombre completo</Label>
              <Input {...form.register("fullName")} />
            </div>
            <div>
              <Label>Email</Label>
              <Input type="email" {...form.register("email")} />
            </div>
            <div>
              <Label>Rol</Label>
              <Select {...form.register("role")}>
                {staffRoleOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>
            <Button type="submit" disabled={inviteMutation.isPending}>
              Enviar invitación
            </Button>
          </form>
          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        </CardContent>
      </Card>

      {inviteResult && (
        <Card className="border-teal-200 bg-teal-50">
          <CardHeader>
            <CardTitle>Invitación creada</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>
              <strong>{inviteResult.fullName}</strong> ({labelUserRole(inviteResult.role)}) — expira{" "}
              {formatDate(inviteResult.expiresAt)}
            </p>
            <p className="break-all rounded-lg bg-white p-3 font-mono text-xs">{inviteResult.token}</p>
            <p className="text-slate-600">
              Comparte este token con la persona invitada. Debe usar{" "}
              <strong>/register-invite</strong> para crear su cuenta.
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
