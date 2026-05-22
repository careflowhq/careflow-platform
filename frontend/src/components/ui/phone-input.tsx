"use client";

import { phoneCountries } from "@/lib/phone";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";

type PhoneInputProps = {
  dialCode: string;
  localNumber: string;
  onDialCodeChange: (dial: string) => void;
  onLocalNumberChange: (local: string) => void;
  error?: string;
  id?: string;
};

export function PhoneInput({
  dialCode,
  localNumber,
  onDialCodeChange,
  onLocalNumberChange,
  error,
  id = "phone",
}: PhoneInputProps) {
  const country = phoneCountries.find((c) => c.dial === dialCode) ?? phoneCountries[0];
  const preview =
    localNumber.trim().length > 0
      ? `Se guardará como ${country.flag} +${dialCode} ${localNumber.replace(/\D/g, "")}`
      : `Ejemplo: ${country.flag} +${country.dial} ${country.placeholder}`;

  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>Teléfono / WhatsApp</Label>
      <div className="flex gap-2">
        <Select
          aria-label="País del teléfono"
          className="w-[148px] shrink-0"
          value={dialCode}
          onChange={(e) => onDialCodeChange(e.target.value)}
        >
          {phoneCountries.map((c) => (
            <option key={c.code} value={c.dial}>
              {c.flag} +{c.dial}
            </option>
          ))}
        </Select>
        <Input
          id={id}
          type="tel"
          inputMode="numeric"
          autoComplete="tel-national"
          placeholder={country.placeholder}
          value={localNumber}
          onChange={(e) => onLocalNumberChange(e.target.value)}
          className={cn(error && "border-red-400 focus-visible:ring-red-400")}
        />
      </div>
      <p className={cn("text-xs", error ? "text-red-600" : "text-slate-500")}>{error ?? preview}</p>
    </div>
  );
}
