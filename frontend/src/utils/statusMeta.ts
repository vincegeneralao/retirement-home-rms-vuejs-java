import type { Component } from "vue";
import { CalendarClock, Car, CircleCheck, CircleX, Play } from "@lucide/vue";
import type { AssignmentStatus } from "@/types/api";

export const STATUS_LABELS: Record<AssignmentStatus, string> = {
  SCHEDULED: "Scheduled",
  EN_ROUTE: "En route",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export const STATUS_TONES: Record<AssignmentStatus, string> = {
  SCHEDULED: "bg-gray-100 text-slate-700 ring-slate-300",
  EN_ROUTE: "bg-amber-100 text-amber-800 ring-amber-300",
  IN_PROGRESS: "bg-sky-100 text-sky-800 ring-sky-300",
  COMPLETED: "bg-green-100 text-emerald-800 ring-emerald-300",
  CANCELLED: "bg-red-100 text-rose-700 ring-rose-300",
};

export const STATUS_ICONS: Record<AssignmentStatus, Component> = {
  SCHEDULED: CalendarClock,
  EN_ROUTE: Car,
  IN_PROGRESS: Play,
  COMPLETED: CircleCheck,
  CANCELLED: CircleX,
};

export const STATUS_ICON_TONES: Record<AssignmentStatus, string> = {
  SCHEDULED: "text-slate-500",
  EN_ROUTE: "text-amber-600",
  IN_PROGRESS: "text-sky-600",
  COMPLETED: "text-emerald-600",
  CANCELLED: "text-rose-600",
};

/** Statuses a worker may pick from their dashboard dropdown. */
export const WORKER_STATUS_OPTIONS: AssignmentStatus[] = ["SCHEDULED", "IN_PROGRESS", "COMPLETED"];
