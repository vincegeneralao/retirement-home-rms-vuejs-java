import { httpClient } from "./client";
import type {
  AuthResponse,
  AssignmentView,
  WorkerView,
  ClientView,
  AvailabilityView,
  OpenShiftView,
  ProblemDetail,
} from "@/types/api";
import type { AxiosResponse } from "axios";

export async function login(username: string, password: string): Promise<AuthResponse> {
  const res = await httpClient.post<AuthResponse>("/auth/login", { username, password });
  return res.data;
}

export async function getWorkerAssignments(date?: string): Promise<AssignmentView[]> {
  const params = date ? { date } : {};
  const res = await httpClient.get<AssignmentView[]>("/worker/assignments/today", { params });
  return res.data;
}

export async function getAdminWorkers(date?: string): Promise<WorkerView[]> {
  const params = date ? { date } : {};
  const res = await httpClient.get<WorkerView[]>("/admin/workers", { params });
  return res.data;
}

export async function getAdminClients(): Promise<ClientView[]> {
  const res = await httpClient.get<ClientView[]>("/admin/clients");
  return res.data;
}

export async function createClient(body: { fullName: string; roomNumber: string; address: string }): Promise<ClientView> {
  const res = await httpClient.post<ClientView>("/admin/clients", body);
  return res.data;
}

export async function getAdminAssignments(date?: string): Promise<AssignmentView[]> {
  const params = date ? { date } : {};
  const res = await httpClient.get<AssignmentView[]>("/admin/assignments/today", { params });
  return res.data;
}

export async function createWorker(body: { username: string; fullName: string; password: string }): Promise<WorkerView> {
  const res = await httpClient.post<WorkerView>("/admin/workers", body);
  return res.data;
}

export async function deactivateWorker(workerId: number): Promise<WorkerView> {
  const res = await httpClient.delete<WorkerView>(`/admin/workers/${workerId}`);
  return res.data;
}

/** Sets the worker's password back to blank and forces them to deal with it on next sign-in. */
export async function resetWorkerPassword(workerId: number): Promise<void> {
  await httpClient.post(`/admin/workers/${workerId}/reset-password`);
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<AuthResponse> {
  const res = await httpClient.post<AuthResponse>("/worker/password/change", { currentPassword, newPassword });
  return res.data;
}

/** Acknowledges the blank reset password and keeps it. */
export async function keepResetPassword(): Promise<AuthResponse> {
  const res = await httpClient.post<AuthResponse>("/worker/password/keep");
  return res.data;
}

export async function getWorkerAvailability(workerId: number): Promise<AvailabilityView[]> {
  const res = await httpClient.get<AvailabilityView[]>(`/admin/workers/${workerId}/availability`);
  return res.data;
}

export async function addWorkerAvailability(
  workerId: number,
  body: { dayOfWeek: string; startTime: string; endTime: string },
): Promise<AvailabilityView> {
  const res = await httpClient.post<AvailabilityView>(`/admin/workers/${workerId}/availability`, body);
  return res.data;
}

export async function removeWorkerAvailability(workerId: number, availabilityId: number): Promise<void> {
  await httpClient.delete(`/admin/workers/${workerId}/availability/${availabilityId}`);
}

export interface ReportDownload {
  blob: Blob;
  filename: string;
}

export async function downloadDailyReport(date?: string): Promise<ReportDownload> {
  const params = date ? { date } : {};
  const res = await httpClient.get<Blob>("/admin/reports/daily/download", { params, responseType: "blob" });
  const disposition: string = res.headers?.["content-disposition"] ?? "";
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  const filename = utf8 ? decodeURIComponent(utf8[1]) : plain ? plain[1] : "daily-report.xlsx";
  return { blob: res.data, filename };
}

export async function getWorkerUpcoming(): Promise<AssignmentView[]> {
  const res = await httpClient.get<AssignmentView[]>("/worker/assignments/upcoming");
  return res.data;
}

export async function updateWorkerAssignmentStatus(assignmentId: number, status: string): Promise<AssignmentView> {
  const res = await httpClient.patch<AssignmentView>(`/worker/assignments/${assignmentId}/status`, { status });
  return res.data;
}

export async function getOpenShifts(): Promise<OpenShiftView[]> {
  const res = await httpClient.get<OpenShiftView[]>("/worker/open-shifts");
  return res.data;
}

export async function claimOpenShift(openShiftId: number): Promise<OpenShiftView> {
  const res = await httpClient.post<OpenShiftView>(`/worker/open-shifts/${openShiftId}/claim`);
  return res.data;
}

export async function getAdminOpenShifts(): Promise<OpenShiftView[]> {
  const res = await httpClient.get<OpenShiftView[]>("/admin/open-shifts");
  return res.data;
}

export async function createOpenShift(body: {
  clientId: number;
  date: string;
  startTime: string;
  endTime: string;
  notes?: string;
}): Promise<OpenShiftView> {
  const res = await httpClient.post<OpenShiftView>("/admin/open-shifts", body);
  return res.data;
}

export async function assignOpenShift(openShiftId: number, workerId: number): Promise<OpenShiftView> {
  const res = await httpClient.post<OpenShiftView>(`/admin/open-shifts/${openShiftId}/assign`, { workerId });
  return res.data;
}

export async function cancelOpenShift(openShiftId: number): Promise<OpenShiftView> {
  const res = await httpClient.delete<OpenShiftView>(`/admin/open-shifts/${openShiftId}`);
  return res.data;
}
