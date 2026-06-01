/**
 * API client for the Med Lion HR local server.
 * Falls back to localStorage when the Express server isn't running (e.g. in Rork's preview).
 */

import { localStore } from "./localStore";

export interface GeoPoint {
  latitude: number;
  longitude: number;
}

export interface WorkSite {
  name: string;
  center: GeoPoint;
  radiusMeters: number;
}

export type PresenceStatus = "CHECKED_IN" | "CHECKED_OUT" | "ON_LEAVE";

export interface Employee {
  id: string;
  employeeId: string;
  password: string;
  name: string;
  role: string;
  avatarColor: number;
  baseSalary: number;
  status: PresenceStatus;
}

export interface AttendanceLog {
  id: string;
  employeeId: string;
  checkInMillis: number;
  checkOutMillis: number | null;
  location: GeoPoint;
  distanceFromSite: number;
  verified: boolean;
}

export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED";
export type LeavePayType = "WITH_PAY" | "WITHOUT_PAY";

export interface LeaveRequest {
  id: string;
  employeeId: string;
  employeeName: string;
  startMillis: number;
  endMillis: number;
  reason: string;
  type: string;
  status: LeaveStatus;
  payType: LeavePayType | null;
  requestedAtMillis: number;
}

export interface SalaryBreakdown {
  employeeId: string;
  employeeName: string;
  baseSalary: number;
  totalWorkingDays: number;
  daysWorked: number;
  paidLeaveDays: number;
  unpaidLeaveDays: number;
  absentDays: number;
  perDayRate: number;
  grossEarned: number;
  deductions: number;
  netPayable: number;
  dailyWorkingHours: number;
  month: number;
  year: number;
}

export interface YearlySummary {
  employeeId: string;
  employeeName: string;
  baseSalary: number;
  monthlyNet: number[];
  totalNet: number;
}

function isNetworkError(err: unknown): boolean {
  const msg = err instanceof Error ? err.message : String(err);
  return (
    msg.includes("fetch failed") ||
    msg.includes("NetworkError") ||
    msg.includes("Failed to fetch") ||
    msg.includes("404") ||
    msg.includes("500")
  );
}

async function tryServer<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(body.error || `Request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

/** Try the server first; on network/404 errors, fall back to localStorage. */
async function withFallback<T>(serverCall: () => Promise<T>, localCall: () => T): Promise<T> {
  try {
    return await serverCall();
  } catch (err) {
    if (isNetworkError(err)) return localCall();
    throw err;
  }
}

export const api = {
  health: () => tryServer<{ ok: boolean; name: string }>("/health"),

  adminLogin: (password: string) =>
    withFallback(
      () => tryServer<{ ok: boolean; token: string }>("/admin/login", {
        method: "POST",
        body: JSON.stringify({ password }),
      }),
      () => localStore.adminLogin(password),
    ),

  changeAdminPassword: (password: string) =>
    withFallback(
      () => tryServer<{ ok: boolean }>("/admin/change-password", {
        method: "POST",
        body: JSON.stringify({ password }),
      }),
      () => localStore.changeAdminPassword(password),
    ),

  employees: () =>
    withFallback(
      () => tryServer<Employee[]>("/employees"),
      () => localStore.employees(),
    ),

  createEmployee: (data: { employeeId: string; password: string; name: string; role: string; baseSalary: number }) =>
    withFallback(
      () => tryServer<{ ok: boolean; employee: Employee }>("/employees", {
        method: "POST",
        body: JSON.stringify(data),
      }),
      () => localStore.createEmployee(data),
    ),

  deleteEmployee: (id: string) =>
    withFallback(
      () => tryServer<{ ok: boolean }>(`/employees/${id}`, { method: "DELETE" }),
      () => localStore.deleteEmployee(id),
    ),

  attendance: (employeeId?: string, month?: number, year?: number) => {
    const serverCall = async () => {
      const params = new URLSearchParams();
      if (employeeId) params.set("employeeId", employeeId);
      if (month !== undefined) params.set("month", String(month));
      if (year !== undefined) params.set("year", String(year));
      const q = params.toString();
      return tryServer<AttendanceLog[]>(`/attendance${q ? `?${q}` : ""}`);
    };
    return withFallback(serverCall, () => localStore.attendance(employeeId, month, year));
  },

  updateAttendance: (id: string, checkInMillis: number, checkOutMillis: number | null) =>
    tryServer<{ ok: boolean }>(`/attendance/${id}`, {
      method: "PUT",
      body: JSON.stringify({ checkInMillis, checkOutMillis }),
    }),

  leaves: () =>
    withFallback(
      () => tryServer<LeaveRequest[]>("/leaves"),
      () => localStore.leaves(),
    ),

  decideLeave: (id: string, status: LeaveStatus, payType: LeavePayType | null) =>
    withFallback(
      () => tryServer<{ ok: boolean }>(`/leaves/${id}`, {
        method: "PUT",
        body: JSON.stringify({ status, payType }),
      }),
      () => localStore.decideLeave(id, status, payType),
    ),

  salaries: (month: number, year: number) =>
    withFallback(
      () => tryServer<SalaryBreakdown[]>(`/salaries/${month}/${year}`),
      () => localStore.salaries(month, year),
    ),

  summaries: (year: number) =>
    withFallback(
      () => tryServer<YearlySummary[]>(`/summaries/${year}`),
      () => localStore.summaries(year),
    ),

  worksite: () =>
    withFallback(
      () => tryServer<WorkSite>("/worksite"),
      () => localStore.worksite(),
    ),

  updateWorksite: (data: { name: string; lat: number; lon: number; radius: number }) =>
    withFallback(
      () => tryServer<{ ok: boolean; workSite: WorkSite }>("/worksite", {
        method: "PUT",
        body: JSON.stringify(data),
      }),
      () => localStore.updateWorksite(data),
    ),
};

export function formatMoney(value: number): string {
  return "₹" + Math.round(value).toLocaleString("en-IN");
}

export const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
export const SHORT_MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
