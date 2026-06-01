/**
 * Client-side localStorage mirror of the server's data store.
 * Used as a fallback when the Express server isn't running (e.g. in Rork's preview).
 */

import type { Employee, AttendanceLog, LeaveRequest, WorkSite, SalaryBreakdown, YearlySummary, GeoPoint, LeavePayType, LeaveStatus } from "./api";

interface AppData {
  adminPassword: string;
  workSite: WorkSite;
  employees: Employee[];
  logs: AttendanceLog[];
  leaves: LeaveRequest[];
}

const DAILY_WORKING_HOURS = 8;
const STORAGE_KEY = "med_lion_hr_data";

const DEFAULT_DATA: AppData = {
  adminPassword: "Yashwant@2000",
  workSite: {
    name: "Sonorous — Vapi",
    center: { latitude: 20.3734, longitude: 72.9141 },
    radiusMeters: 1000,
  },
  employees: [
    { id: "e1", employeeId: "EMP001", password: "pass1234", name: "Aria Nakamura", role: "Lead Product Designer", avatarColor: 0xFF26E8FF, baseSalary: 85000, status: "CHECKED_OUT" },
    { id: "e2", employeeId: "EMP002", password: "pass1234", name: "Marcus Vela", role: "Backend Engineer", avatarColor: 0xFF9D5BFF, baseSalary: 75000, status: "CHECKED_IN" },
    { id: "e3", employeeId: "EMP003", password: "pass1234", name: "Lena Frost", role: "QA Analyst", avatarColor: 0xFFFF4ECB, baseSalary: 55000, status: "CHECKED_IN" },
    { id: "e4", employeeId: "EMP004", password: "pass1234", name: "Dev Okafor", role: "DevOps", avatarColor: 0xFF5BFFB0, baseSalary: 80000, status: "ON_LEAVE" },
    { id: "e5", employeeId: "EMP005", password: "pass1234", name: "Sora Pierce", role: "Data Scientist", avatarColor: 0xFFFFC24B, baseSalary: 72000, status: "CHECKED_OUT" },
  ],
  logs: [],
  leaves: [],
};

function loadData(): AppData {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as AppData;
  } catch { /* ignore */ }
  const data = JSON.parse(JSON.stringify(DEFAULT_DATA)) as AppData;
  saveData(data);
  return data;
}

function saveData(data: AppData): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch { /* ignore */ }
}

function genId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function inMonth(millis: number, month: number, year: number): boolean {
  const d = new Date(millis);
  return d.getMonth() === month && d.getFullYear() === year;
}

function dayKey(millis: number): number {
  const d = new Date(millis);
  return d.getFullYear() * 1000 + Math.floor((d.getTime() - new Date(d.getFullYear(), 0, 0).getTime()) / 86400000);
}

function standardWorkingDays(month: number, year: number): number {
  const days = new Date(year, month + 1, 0).getDate();
  let count = 0;
  for (let d = 1; d <= days; d++) {
    const dow = new Date(year, month, d).getDay();
    if (dow !== 0 && dow !== 6) count++;
  }
  return count;
}

function leaveDays(l: LeaveRequest): number {
  return Math.max(1, Math.floor((l.endMillis - l.startMillis) / 86_400_000) + 1);
}

function salaryFor(emp: Employee, month: number, year: number): SalaryBreakdown {
  const data = loadData();
  const totalDays = standardWorkingDays(month, year);
  const perDay = totalDays > 0 ? emp.baseSalary / totalDays : 0;

  const empLogs = data.logs.filter(l => l.employeeId === emp.id && inMonth(l.checkInMillis, month, year));
  const dayGroups = new Map<number, AttendanceLog[]>();
  for (const l of empLogs) {
    const key = dayKey(l.checkInMillis);
    if (!dayGroups.has(key)) dayGroups.set(key, []);
    dayGroups.get(key)!.push(l);
  }

  let fractionalDays = 0;
  for (const [, dayLogs] of dayGroups) {
    const totalMs = dayLogs.reduce((sum, l) => sum + ((l.checkOutMillis ?? Date.now()) - l.checkInMillis), 0);
    const hours = totalMs / 3_600_000;
    fractionalDays += Math.min(hours, DAILY_WORKING_HOURS) / DAILY_WORKING_HOURS;
  }

  const approved = data.leaves.filter(l =>
    l.employeeId === emp.id && l.status === "APPROVED" && inMonth(l.startMillis, month, year)
  );
  const paidLeaveDays = approved.filter(l => l.payType === "WITH_PAY").reduce((s, l) => s + leaveDays(l), 0);
  const unpaidLeaveDays = approved.filter(l => l.payType === "WITHOUT_PAY").reduce((s, l) => s + leaveDays(l), 0);

  const accountedDays = fractionalDays + paidLeaveDays + unpaidLeaveDays;
  const absentDays = Math.max(0, totalDays - accountedDays);
  const gross = perDay * (fractionalDays + paidLeaveDays);

  return {
    employeeId: emp.id, employeeName: emp.name, baseSalary: emp.baseSalary,
    totalWorkingDays: totalDays, daysWorked: Math.round(fractionalDays * 100) / 100,
    paidLeaveDays, unpaidLeaveDays,
    absentDays: Math.round(absentDays * 100) / 100,
    perDayRate: Math.round(perDay * 100) / 100,
    grossEarned: Math.round(gross * 100) / 100,
    deductions: Math.round((perDay * (unpaidLeaveDays + absentDays)) * 100) / 100,
    netPayable: Math.round(gross * 100) / 100,
    dailyWorkingHours: DAILY_WORKING_HOURS,
    month, year,
  };
}

export const localStore = {
  adminLogin: (password: string): { ok: boolean; token: string } => {
    const data = loadData();
    if (password === data.adminPassword) {
      return { ok: true, token: "admin-session-local" };
    }
    throw new Error("Invalid password");
  },

  changeAdminPassword: (password: string): { ok: boolean } => {
    const data = loadData();
    data.adminPassword = password;
    saveData(data);
    return { ok: true };
  },

  employees: (): Employee[] => {
    return loadData().employees;
  },

  createEmployee: (body: { employeeId: string; password: string; name: string; role: string; baseSalary: number }): { ok: boolean; employee: Employee } => {
    const data = loadData();
    if (data.employees.some(e => e.employeeId === body.employeeId)) {
      throw new Error("Employee ID already exists");
    }
    const colors = [0xFF26E8FF, 0xFF9D5BFF, 0xFFFF4ECB, 0xFF5BFFB0, 0xFFFFC24B, 0xFF3B82F6, 0xFFE040FB];
    const emp: Employee = {
      id: genId("emp"),
      employeeId: body.employeeId,
      password: body.password,
      name: body.name,
      role: body.role || "Staff",
      avatarColor: colors[Math.floor(Math.random() * colors.length)],
      baseSalary: body.baseSalary || 0,
      status: "CHECKED_OUT",
    };
    data.employees.push(emp);
    saveData(data);
    return { ok: true, employee: emp };
  },

  deleteEmployee: (id: string): { ok: boolean } => {
    const data = loadData();
    data.employees = data.employees.filter(e => e.id !== id);
    data.logs = data.logs.filter(l => l.employeeId !== id);
    data.leaves = data.leaves.filter(l => l.employeeId !== id);
    saveData(data);
    return { ok: true };
  },

  attendance: (employeeId?: string, month?: number, year?: number): AttendanceLog[] => {
    let logs = loadData().logs;
    if (employeeId) logs = logs.filter(l => l.employeeId === employeeId);
    if (month !== undefined && year !== undefined) {
      logs = logs.filter(l => inMonth(l.checkInMillis, month, year));
    }
    logs.sort((a, b) => b.checkInMillis - a.checkInMillis);
    return logs;
  },

  leaves: (): LeaveRequest[] => {
    const leaves = [...loadData().leaves];
    leaves.sort((a, b) => b.requestedAtMillis - a.requestedAtMillis);
    return leaves;
  },

  decideLeave: (id: string, status: LeaveStatus, payType: LeavePayType | null): { ok: boolean } => {
    const data = loadData();
    data.leaves = data.leaves.map(l => {
      if (l.id !== id) return l;
      return { ...l, status, payType: status === "APPROVED" ? payType : null };
    });
    saveData(data);
    return { ok: true };
  },

  salaries: (month: number, year: number): SalaryBreakdown[] => {
    return loadData().employees.map(e => salaryFor(e, month, year));
  },

  summaries: (year: number): YearlySummary[] => {
    return loadData().employees.map(emp => {
      const monthly = Array.from({ length: 12 }, (_, m) => salaryFor(emp, m, year).netPayable);
      return {
        employeeId: emp.id, employeeName: emp.name, baseSalary: emp.baseSalary,
        monthlyNet: monthly, totalNet: monthly.reduce((a, b) => a + b, 0),
      };
    });
  },

  worksite: (): WorkSite => {
    return loadData().workSite;
  },

  updateWorksite: (data: { name: string; lat: number; lon: number; radius: number }): { ok: boolean; workSite: WorkSite } => {
    const appData = loadData();
    appData.workSite = {
      name: data.name || appData.workSite.name,
      center: { latitude: data.lat, longitude: data.lon },
      radiusMeters: data.radius,
    };
    saveData(appData);
    return { ok: true, workSite: appData.workSite };
  },
};
