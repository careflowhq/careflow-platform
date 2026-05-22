"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { registerInvite } from "@/lib/api/auth";
import { getErrorMessage } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const schema = z
  .object({
    token: z.string().min(1, "Token requerido"),
    password: z.string().min(6, "Mínimo 6 caracteres"),
    confirm: z.string(),
  })
  .refine((d) => d.password === d.confirm, { message: "Las contraseñas no coinciden", path: ["confirm"] });

type FormData = z.infer<typeof schema>;

function RegisterInviteForm() {
  const router = useRouter();
  const params = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { token: params.get("token") ?? "" },
  });

  const onSubmit = async (data: FormData) => {
    setError(null);
    try {
      await registerInvite(data.token, data.password);
      setSuccess(true);
      setTimeout(() => router.replace("/login"), 1500);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Completar registro</CardTitle>
      </CardHeader>
      <CardContent>
        {success ? (
          <p className="text-sm text-emerald-700">Cuenta creada. Redirigiendo al login…</p>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <Label htmlFor="token">Token de invitación</Label>
              <Input id="token" {...register("token")} />
            </div>
            <div>
              <Label htmlFor="password">Nueva contraseña</Label>
              <Input id="password" type="password" {...register("password")} />
            </div>
            <div>
              <Label htmlFor="confirm">Confirmar contraseña</Label>
              <Input id="confirm" type="password" {...register("confirm")} />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Creando cuenta…" : "Crear cuenta"}
            </Button>
          </form>
        )}
        <p className="mt-4 text-center text-sm text-slate-600">
          <Link href="/login" className="text-teal-700 hover:underline">
            Volver al login
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

export default function RegisterInvitePage() {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <Suspense fallback={<div className="text-slate-500">Cargando…</div>}>
        <RegisterInviteForm />
      </Suspense>
    </div>
  );
}
