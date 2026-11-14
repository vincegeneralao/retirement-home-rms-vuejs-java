/**
 * Site-local time utilities: the facility uses wall-clock timestamps,
 * so we never convert via Date constructor for display purposes.
 */

export function formatDateLabel(date: Date): string {
  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  return `${days[date.getDay()]} ${String(date.getDate()).padStart(2, "0")} ${months[date.getMonth()]} ${date.getFullYear()}`;
}

export function toIsoDate(d: Date): string {
  const y = d.getFullYear();
  const mo = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${mo}-${day}`;
}

export function todayIso(): string {
  return toIsoDate(new Date());
}

/**
 * Extract HH:mm from an ISO-like start string "2026-11-02T08:00:00".
 */
export function timeLabel(iso: string): string {
  return iso.slice(11, 16);
}

export function dateLabel(iso: string): string {
  return iso.slice(0, 10);
}
