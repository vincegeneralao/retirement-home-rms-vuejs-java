export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  username: string;
  fullName: string;
  roles: string[];
  expiresAt: string;
  /** True when an admin reset the password and the worker has not replaced or kept it yet. */
  mustChangePassword: boolean;
}

export interface AssignmentView {
  id: number;
  workerId: number;
  workerName: string;
  clientId: number;
  clientName: string;
  roomNumber: string;
  address: string;
  location: string;
  start: string;
  end: string;
  durationMinutes: number;
  status: AssignmentStatus;
  notes: string | null;
}

export type AssignmentStatus = "SCHEDULED" | "EN_ROUTE" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface AvailabilityView {
  id: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
}

export type OpenShiftStatus = "OPEN" | "TAKEN" | "CANCELLED";

export interface OpenShiftView {
  id: number;
  clientId: number;
  clientName: string;
  roomNumber: string;
  address: string;
  location: string;
  start: string;
  end: string;
  durationMinutes: number;
  status: OpenShiftStatus;
  notes: string | null;
  takenById: number | null;
  takenByName: string;
  takenAt: string | null;
  assignmentId: number | null;
}

export interface WorkerView {
  id: number;
  fullName: string;
  username: string;
  role: string;
  enabled: boolean;
  shiftsToday: number;
  availability: AvailabilityView[];
}

export interface ClientView {
  id: number;
  fullName: string;
  roomNumber: string;
  address: string;
  location: string;
  active: boolean;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  code?: string;
}
