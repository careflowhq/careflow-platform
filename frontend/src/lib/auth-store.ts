"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import { decodeToken, isTokenExpired } from "@/lib/jwt";
import type { JwtClaims, UserRole } from "@/types";

interface AuthState {
  token: string | null;
  setToken: (token: string | null) => void;
  logout: () => void;
  getClaims: () => JwtClaims | null;
  getRole: () => UserRole | null;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      setToken: (token) => set({ token }),
      logout: () => set({ token: null }),
      getClaims: () => {
        const token = get().token;
        if (!token || isTokenExpired(token)) return null;
        try {
          return decodeToken(token);
        } catch {
          return null;
        }
      },
      getRole: () => get().getClaims()?.role ?? null,
      isAuthenticated: () => {
        const token = get().token;
        return !!token && !isTokenExpired(token);
      },
    }),
    { name: "careflow-auth" }
  )
);
