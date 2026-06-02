/**
 * API client for the Med Lion HR cloud backend.
 * All data is stored in Supabase Postgres, served via Cloudflare Worker.
 */

/** Cloud Worker API base URL — persistent cloud backend, no local server needed. */
const API_BASE: string = "https://li980wrgnunptwig2nzqh-backend.rork.app";

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

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}/api${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(body.error || `Request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  health: () => request<{ ok: boolean; name: string }>("/health"),

  adminLogin: (userId: string, password: string) =>
    request<{ ok: boolean; token: string }>("/admin/login", {
      method: "POST",
      body: JSON.stringify({ userId, password }),
    }),

  changeAdminPassword: (password: string) =>
    request<{ ok: boolean }>("/admin/change-password", {
      method: "POST",
      body: JSON.stringify({ password }),
    }),

  employees: () =>
    request<Employee[]>("/employees"),

  createEmployee: (data: { employeeId: string; password: string; name: string; role: string; baseSalary: number }) =>
    request<{ ok: boolean; employee: Employee }>("/employees", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  deleteEmployee: (id: string) =>
    request<{ ok: boolean }>(`/employees/${id}`, { method: "DELETE" }),

  attendance: (employeeId?: string, month?: number, year?: number) => {
    const params = new URLSearchParams();
    if (employeeId) params.set("employeeId", employeeId);
    if (month !== undefined) params.set("month", String(month));
    if (year !== undefined) params.set("year", String(year));
    const q = params.toString();
    return request<AttendanceLog[]>(`/attendance${q ? `?${q}` : ""}`);
  },

  updateAttendance: (id: string, checkInMillis: number, checkOutMillis: number | null) =>
    request<{ ok: boolean }>(`/attendance/${id}`, {
      method: "PUT",
      body: JSON.stringify({ checkInMillis, checkOutMillis }),
    }),

  leaves: () =>
    request<LeaveRequest[]>("/leaves"),

  decideLeave: (id: string, status: LeaveStatus, payType: LeavePayType | null) =>
    request<{ ok: boolean }>(`/leaves/${id}`, {
      method: "PUT",
      body: JSON.stringify({ status, payType }),
    }),

  salaries: (month: number, year: number) =>
    request<SalaryBreakdown[]>(`/salaries/${month}/${year}`),

  summaries: (year: number) =>
    request<YearlySummary[]>(`/summaries/${year}`),

  worksite: () =>
    request<WorkSite>("/worksite"),

  updateWorksite: (data: { name: string; lat: number; lon: number; radius: number }) =>
    request<{ ok: boolean; workSite: WorkSite }>("/worksite", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
};

export function formatMoney(value: number): string {
  return "₹" + Math.round(value).toLocaleString("en-IN");
}

export const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
export const SHORT_MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
