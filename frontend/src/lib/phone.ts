export type PhoneCountry = {
  code: string;
  flag: string;
  dial: string;
  label: string;
  placeholder: string;
  localLength: number;
};

/** Países LATAM soportados en el MVP (default: Perú). */
export const phoneCountries: PhoneCountry[] = [
  { code: "PE", flag: "🇵🇪", dial: "51", label: "Perú", placeholder: "987 654 321", localLength: 9 },
  { code: "MX", flag: "🇲🇽", dial: "52", label: "México", placeholder: "55 1234 5678", localLength: 10 },
  { code: "CO", flag: "🇨🇴", dial: "57", label: "Colombia", placeholder: "300 123 4567", localLength: 10 },
  { code: "AR", flag: "🇦🇷", dial: "54", label: "Argentina", placeholder: "11 2345 6789", localLength: 10 },
  { code: "CL", flag: "🇨🇱", dial: "56", label: "Chile", placeholder: "9 1234 5678", localLength: 9 },
  { code: "EC", flag: "🇪🇨", dial: "593", label: "Ecuador", placeholder: "99 123 4567", localLength: 9 },
];

export const defaultPhoneCountry = phoneCountries[0];

export function getPhoneCountry(dial: string): PhoneCountry {
  return phoneCountries.find((c) => c.dial === dial) ?? defaultPhoneCountry;
}

/** Solo dígitos del número local (sin código de país). */
export function digitsOnly(value: string): string {
  return value.replace(/\D/g, "");
}

/** Normaliza a formato E.164 para guardar y WhatsApp: +51987654321 */
export function normalizePhone(dialCode: string, localNumber: string): string {
  let local = digitsOnly(localNumber);
  if (local.startsWith("0")) {
    local = local.slice(1);
  }
  return `+${dialCode}${local}`;
}

/** Intenta separar un teléfono guardado en código de país + número local. */
export function parsePhone(stored: string): { dialCode: string; localNumber: string } {
  const digits = digitsOnly(stored);
  if (!digits) {
    return { dialCode: defaultPhoneCountry.dial, localNumber: "" };
  }

  const sorted = [...phoneCountries].sort((a, b) => b.dial.length - a.dial.length);
  for (const country of sorted) {
    if (digits.startsWith(country.dial)) {
      return {
        dialCode: country.dial,
        localNumber: digits.slice(country.dial.length),
      };
    }
  }

  if (digits.length === defaultPhoneCountry.localLength && digits.startsWith("9")) {
    return { dialCode: defaultPhoneCountry.dial, localNumber: digits };
  }

  return { dialCode: defaultPhoneCountry.dial, localNumber: digits };
}

/** Muestra +51 947 352 743 en listados. */
export function formatPhoneDisplay(stored: string): string {
  const { dialCode, localNumber } = parsePhone(stored);
  const country = getPhoneCountry(dialCode);
  const local = digitsOnly(localNumber);
  if (!local) return stored;

  if (country.code === "PE" && local.length === 9) {
    return `+${dialCode} ${local.slice(0, 3)} ${local.slice(3, 6)} ${local.slice(6)}`;
  }

  return `+${dialCode} ${local.replace(/(\d{3})(?=\d)/g, "$1 ").trim()}`;
}

export function validateLocalPhone(dialCode: string, localNumber: string): string | null {
  const country = getPhoneCountry(dialCode);
  const local = digitsOnly(localNumber);
  if (!local) return "Teléfono requerido";
  if (local.length < country.localLength - 1) {
    return `Ingresa un número válido para ${country.label}`;
  }
  if (country.code === "PE" && !local.startsWith("9")) {
    return "En Perú el celular suele empezar con 9";
  }
  return null;
}
