import { cn } from "@/lib/utils";
import type { FollowUpStatus, PatientStatus } from "@/types";

const statusStyles: Record<string, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800",
  AT_RISK: "bg-amber-100 text-amber-800",
  INACTIVE: "bg-slate-100 text-slate-700",
  PENDING: "bg-sky-100 text-sky-800",
  COMPLETED: "bg-emerald-100 text-emerald-800",
  MISSED: "bg-red-100 text-red-800",
  CANCELLED: "bg-slate-100 text-slate-600",
  READY: "bg-teal-100 text-teal-800",
  SENT: "bg-emerald-100 text-emerald-800",
  FAILED: "bg-red-100 text-red-800",
};

export function Badge({
  label,
  display,
  className,
}: {
  label: PatientStatus | FollowUpStatus | string;
  display?: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium",
        statusStyles[label] ?? "bg-slate-100 text-slate-700",
        className
      )}
    >
      {display ?? label}
    </span>
  );
}
