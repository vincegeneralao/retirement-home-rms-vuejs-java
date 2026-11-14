import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/** Merge class names with Tailwind conflict resolution (shadcn-vue convention). */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
