import { jwtDecode } from "jwt-decode";
import type { JwtClaims } from "@/types";

export function decodeToken(token: string): JwtClaims {
  return jwtDecode<JwtClaims>(token);
}

export function isTokenExpired(token: string): boolean {
  try {
    const { exp } = decodeToken(token);
    return Date.now() >= exp * 1000;
  } catch {
    return true;
  }
}
