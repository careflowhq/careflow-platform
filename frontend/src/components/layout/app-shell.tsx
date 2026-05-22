"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Bell,
  CalendarClock,
  LayoutDashboard,
  LogOut,
  Stethoscope,
  Users,
  UsersRound,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { labelUserRole } from "@/lib/labels";
import { useAuthStore } from "@/lib/auth-store";
import { Button } from "@/components/ui/button";

const nav = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/patients", label: "Pacientes", icon: Users },
  { href: "/followups", label: "Seguimientos", icon: CalendarClock },
  { href: "/notifications", label: "Notificaciones", icon: Bell },
  { href: "/team", label: "Equipo", icon: UsersRound, adminOnly: true },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const logout = useAuthStore((s) => s.logout);
  const role = useAuthStore((s) => s.getRole());

  const handleLogout = () => {
    logout();
    router.replace("/login");
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
          <div className="flex items-center gap-2">
            <Stethoscope className="h-6 w-6 text-teal-700" />
            <span className="text-lg font-bold text-slate-900">CareFlow</span>
          </div>
          <div className="flex items-center gap-3 text-sm text-slate-600">
            <span className="rounded-full bg-teal-50 px-3 py-1 font-medium text-teal-800">
              {labelUserRole(role)}
            </span>
            <Button variant="ghost" onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Salir
            </Button>
          </div>
        </div>
      </header>

      <div className="mx-auto grid max-w-7xl gap-6 px-4 py-6 lg:grid-cols-[220px_1fr]">
        <nav className="space-y-1">
          {nav
            .filter((item) => !item.adminOnly || role === "CLINIC_ADMIN")
            .map(({ href, label, icon: Icon }) => (
              <Link
                key={href}
                href={href}
                className={cn(
                  "flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition",
                  pathname === href
                    ? "bg-teal-700 text-white"
                    : "text-slate-700 hover:bg-slate-100"
                )}
              >
                <Icon className="h-4 w-4" />
                {label}
              </Link>
            ))}
        </nav>
        <main>{children}</main>
      </div>
    </div>
  );
}
