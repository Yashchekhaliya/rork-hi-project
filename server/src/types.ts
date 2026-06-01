export interface GeoPoint {
  latitude: number;
  longitude: number;
}

export interface WorkSite {
  name: string;
  center: GeoPoint;
  radiusMeters: number;
}

export interface Employee {
  id: string;
  employeeId: string;
  password: string;
  name: string;
  role: string;
  avatarColor: number;
  baseSalary: number;
  status: "CHECKED_IN" | "CHECKED_OUT" | "ON_LEAVE";
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

export interface LeaveRequest {
  id: string;
  employeeId: string;
  employeeName: string;
  startMillis: number;
  endMillis: number;
  reason: string;
  type: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  payType: "WITH_PAY" | "WITHOUT_PAY" | null;
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

export interface NewEmployeeRequest {
  employeeId: string;
  password: string;
  name: string;
  role: string;
  baseSalary: number;
}

export interface AppData {
  adminPassword: string;
  workSite: WorkSite;
  employees: Employee[];
  logs: AttendanceLog[];
  leaves: LeaveRequest[];
}
