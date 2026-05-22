"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Stethoscope } from "lucide-react";
import { login, registerClinic } from "@/lib/api/auth";
import { getErrorMessage } from "@/lib/api/client";
import { useAuthStore } from "@/lib/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const schema = z
  .object({
    fullName: z.string().min(1, "Nombre requerido"),
    email: z.email("Email inválido"),
    password: z.string().min(6, "Mínimo 6 caracteres"),
    confirm: z.string(),
    clinicName: z.string().min(1, "Nombre del consultorio requerido"),
    country: z.string().min(1, "País requerido"),
    timezone: z.string().min(1, "Zona horaria requerida"),
  })
  .refine((d) => d.password === d.confirm, {
    message: "Las contraseñas no coinciden",
    path: ["confirm"],
  });

type FormData = z.infer<typeof schema>;

const countries = [
  { value: "PE", label: "Perú" },
  { value: "MX", label: "México" },
  { value: "CO", label: "Colombia" },
  { value: "AR", label: "Argentina" },
  { value: "CL", label: "Chile" },
  { value: "EC", label: "Ecuador" },
];

const timezones = [
  { value: "America/Lima", label: "Lima (PET)" },
  { value: "America/Mexico_City", label: "Ciudad de México (CST)" },
  { value: "America/Bogota", label: "Bogotá (COT)" },
  { value: "America/Argentina/Buenos_Aires", label: "Buenos Aires (ART)" },
  { value: "America/Santiago", label: "Santiago (CLT)" },
];

export default function RegisterPage() {
  const router = useRouter();
  const setToken = useAuthStore((s) => s.setToken);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      fullName: "",
      email: "",
      password: "",
      confirm: "",
      clinicName: "",
      country: "PE",
      timezone: "America/Lima",
    },
  });

  const onSubmit = async (data: FormData) => {
    setError(null);
    try {
      await registerClinic({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
        clinicName: data.clinicName,
        country: data.country,
        timezone: data.timezone,
      });
      const { token } = await login(data.email, data.password);
      setToken(token);
      router.replace("/dashboard");
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-8">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <div className="mb-2 flex items-center gap-2 text-teal-700">
            <Stethoscope className="h-6 w-6" />
            <span className="font-semibold">CareFlow</span>
          </div>
          <CardTitle>Registrar consultorio</CardTitle>
          <p className="text-sm text-slate-600">
            Crea tu cuenta como administrador y configura tu consultorio en CareFlow.
          </p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <Label htmlFor="clinicName">Nombre del consultorio</Label>
              <Input id="clinicName" placeholder="Consultorio San Martín" {...register("clinicName")} />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="country">País</Label>
                <Select id="country" {...register("country")}>
                  {countries.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div>
                <Label htmlFor="timezone">Zona horaria</Label>
                <Select id="timezone" {...register("timezone")}>
                  {timezones.map((tz) => (
                    <option key={tz.value} value={tz.value}>
                      {tz.label}
                    </option>
                  ))}
                </Select>
              </div>
            </div>
            <div>
              <Label htmlFor="fullName">Tu nombre completo</Label>
              <Input id="fullName" {...register("fullName")} />
            </div>
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" autoComplete="email" {...register("email")} />
            </div>
            <div>
              <Label htmlFor="password">Contraseña</Label>
              <Input id="password" type="password" autoComplete="new-password" {...register("password")} />
            </div>
            <div>
              <Label htmlFor="confirm">Confirmar contraseña</Label>
              <Input id="confirm" type="password" autoComplete="new-password" {...register("confirm")} />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Creando consultorio…" : "Crear consultorio"}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-600">
            ¿Ya tienes cuenta?{" "}
            <Link href="/login" className="font-medium text-teal-700 hover:underline">
              Iniciar sesión
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
