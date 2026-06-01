import express from "express";
import cors from "cors";
import path from "path";
import fs from "fs";
import { loadData, getData, setData, generateId, saveData } from "./store.js";
import type {
  Employee, AttendanceLog, LeaveRequest, SalaryBreakdown,
  YearlySummary, NewEmployeeRequest, LeavePayType, LeaveStatus,
} from "./types.js";

const DAILY_WORKING_HOURS = 8.0;
const PORT = parseInt(process.env.PORT || "8080", 10);

loadData();

function standardWorkingDays(month: number, year: number): number {
  const days = new Date(year, month + 1, 0).getDate();
  let count = 0;
  for (let d = 1; d <= days; d++) {
    const dow = new Date(year, month, d).getDay();
    if (dow !== 0 && dow !== 6) count++;
  }
  return count;
}

function inMonth(millis: number, month: number, year: number): boolean {
  const d = new Date(millis);
  return d.getMonth() === month && d.getFullYear() === year;
}

function dayKey(millis: number): number {
  const d = new Date(millis);
  return d.getFullYear() * 1000 + Math.floor((d.getTime() - new Date(d.getFullYear(), 0, 0).getTime()) / 86400000);
}

function salaryFor(emp: Employee, month: number, year: number): SalaryBreakdown {
  const { logs, leaves } = getData();
  const totalDays = standardWorkingDays(month, year);
  const perDay = totalDays > 0 ? emp.baseSalary / totalDays : 0;

  const empLogs = logs.filter(l => l.employeeId === emp.id && inMonth(l.checkInMillis, month, year));
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

  const approved = leaves.filter(l =>
    l.employeeId === emp.id && l.status === "APPROVED" && inMonth(l.startMillis, month, year)
  );
  const paidLeaveDays = approved.filter(l => l.payType === "WITH_PAY").reduce((s, l) => s + leaveDays(l), 0);
  const unpaidLeaveDays = approved.filter(l => l.payType === "WITHOUT_PAY").reduce((s, l) => s + leaveDays(l), 0);

  const accountedDays = fractionalDays + paidLeaveDays + unpaidLeaveDays;
  const absentDays = Math.max(0, totalDays - accountedDays);
  const gross = perDay * (fractionalDays + paidLeaveDays);
  const deductions = perDay * (unpaidLeaveDays + absentDays);

  return {
    employeeId: emp.id, employeeName: emp.name, baseSalary: emp.baseSalary,
    totalWorkingDays: totalDays, daysWorked: Math.round(fractionalDays * 100) / 100,
    paidLeaveDays, unpaidLeaveDays,
    absentDays: Math.round(absentDays * 100) / 100,
    perDayRate: Math.round(perDay * 100) / 100,
    grossEarned: Math.round(gross * 100) / 100,
    deductions: Math.round(deductions * 100) / 100,
    netPayable: Math.round(gross * 100) / 100,
    dailyWorkingHours: DAILY_WORKING_HOURS,
    month, year,
  };
}

function leaveDays(l: LeaveRequest): number {
  return Math.max(1, Math.floor((l.endMillis - l.startMillis) / 86_400_000) + 1);
}

const app = express();
app.use(cors());
app.use(express.json());

// ── Root route ──
app.get("/", (_req, res) => {
  res.json({
    name: "Med Lion HR Server",
    version: "1.0.0",
    status: "running",
    endpoints: {
      health: "/api/health",
      login: "POST /api/admin/login",
      employees: "GET|POST /api/employees",
      attendance: "GET|POST /api/attendance",
      leaves: "GET|POST /api/leaves",
      salary: "GET /api/salary/:employeeId",
      export: "GET /api/export/:type/:month/:year",
    },
  });
});

// ── Serve web dashboard static files (if built) ──
const webDistPath = path.join(__dirname, "..", "web-dist");
if (fs.existsSync(webDistPath)) {
  app.use(express.static(webDistPath));
  // SPA fallback: serve index.html for any unmatched route
  app.get("*", (_req, res) => {
    res.sendFile(path.join(webDistPath, "index.html"));
  });
}

// ── Health check ──
app.get("/api/health", (_req, res) => {
  res.json({ ok: true, name: "Med Lion HR Server", time: Date.now() });
});

// ═══════════ ADMIN AUTH ═══════════

app.post("/api/admin/login", (req, res) => {
  const { password } = req.body;
  if (password === getData().adminPassword) {
    res.json({ ok: true, token: "admin-session" });
  } else {
    res.status(401).json({ ok: false, error: "Invalid password" });
  }
});

app.post("/api/admin/change-password", (req, res) => {
  const { password } = req.body;
  if (!password || password.length < 4) {
    return res.status(400).json({ ok: false, error: "Password must be at least 4 characters" });
  }
  setData({ adminPassword: password });
  res.json({ ok: true });
});

// ═══════════ EMPLOYEES ═══════════

app.get("/api/employees", (_req, res) => {
  res.json(getData().employees);
});

app.post("/api/employees", (req, res) => {
  const body = req.body as NewEmployeeRequest;
  if (!body.employeeId || !body.password || !body.name || body.password.length < 4) {
    return res.status(400).json({ ok: false, error: "Invalid employee data" });
  }
  const { employees } = getData();
  if (employees.some(e => e.employeeId === body.employeeId)) {
    return res.status(409).json({ ok: false, error: "Employee ID already exists" });
  }
  const colors = [0xFF26E8FF, 0xFF9D5BFF, 0xFFFF4ECB, 0xFF5BFFB0, 0xFFFFC24B, 0xFF3B82F6, 0xFFE040FB];
  const emp: Employee = {
    id: generateId("emp"),
    employeeId: body.employeeId,
    password: body.password,
    name: body.name,
    role: body.role || "Staff",
    avatarColor: colors[Math.floor(Math.random() * colors.length)],
    baseSalary: body.baseSalary || 0,
    status: "CHECKED_OUT",
  };
  setData({ employees: [...employees, emp] });
  res.json({ ok: true, employee: emp });
});

app.delete("/api/employees/:id", (req, res) => {
  const { id } = req.params;
  const { employees, logs, leaves } = getData();
  if (!employees.some(e => e.id === id)) {
    return res.status(404).json({ ok: false, error: "Employee not found" });
  }
  setData({
    employees: employees.filter(e => e.id !== id),
    logs: logs.filter(l => l.employeeId !== id),
    leaves: leaves.filter(l => l.employeeId !== id),
  });
  res.json({ ok: true });
});

app.post("/api/employees/login", (req, res) => {
  const { employeeId, password } = req.body;
  const emp = getData().employees.find(e => e.employeeId === employeeId && e.password === password);
  if (!emp) return res.status(401).json({ ok: false, error: "Invalid credentials" });
  res.json({ ok: true, employee: emp });
});

app.post("/api/employees/:id/change-password", (req, res) => {
  const { id } = req.params;
  const { currentPassword, newPassword } = req.body;
  if (!newPassword || newPassword.length < 4) {
    return res.status(400).json({ ok: false, error: "New password too short" });
  }
  const emp = getData().employees.find(e => e.id === id);
  if (!emp || emp.password !== currentPassword) {
    return res.status(401).json({ ok: false, error: "Invalid current password" });
  }
  setData({
    employees: getData().employees.map(e => e.id === id ? { ...e, password: newPassword } : e),
  });
  res.json({ ok: true });
});

// ═══════════ ATTENDANCE ═══════════

app.get("/api/attendance", (req, res) => {
  const { employeeId, month, year } = req.query;
  let logs = getData().logs;
  if (employeeId) logs = logs.filter(l => l.employeeId === employeeId);
  if (month !== undefined && year !== undefined) {
    const m = parseInt(month as string), y = parseInt(year as string);
    logs = logs.filter(l => inMonth(l.checkInMillis, m, y));
  }
  logs.sort((a, b) => b.checkInMillis - a.checkInMillis);
  res.json(logs);
});

app.post("/api/attendance/checkin", (req, res) => {
  const { employeeId, location } = req.body;
  const { logs, workSite, employees } = getData();

  if (logs.some(l => l.employeeId === employeeId && !l.checkOutMillis)) {
    const existing = logs.find(l => l.employeeId === employeeId && !l.checkOutMillis);
    return res.json({ ok: true, log: existing, message: "Already checked in" });
  }

  const dist = haversine(
    workSite.center.latitude, workSite.center.longitude,
    location.latitude, location.longitude
  );
  if (dist > workSite.radiusMeters) {
    return res.status(403).json({ ok: false, error: "Outside geofence", distance: dist });
  }

  const log: AttendanceLog = {
    id: generateId("log"),
    employeeId,
    checkInMillis: Date.now(),
    checkOutMillis: null,
    location,
    distanceFromSite: dist,
    verified: true,
  };
  setData({
    logs: [...logs, log],
    employees: employees.map(e => e.id === employeeId ? { ...e, status: "CHECKED_IN" } : e),
  });
  res.json({ ok: true, log });
});

app.post("/api/attendance/checkout", (req, res) => {
  const { employeeId } = req.body;
  const { logs, employees } = getData();
  const openIdx = logs.findIndex(l => l.employeeId === employeeId && !l.checkOutMillis);
  if (openIdx === -1) return res.status(400).json({ ok: false, error: "No open session" });

  const updated = [...logs];
  updated[openIdx] = { ...updated[openIdx], checkOutMillis: Date.now() };
  setData({
    logs: updated,
    employees: employees.map(e => e.id === employeeId ? { ...e, status: "CHECKED_OUT" } : e),
  });
  res.json({ ok: true, log: updated[openIdx] });
});

app.put("/api/attendance/:id", (req, res) => {
  const { id } = req.params;
  const { checkInMillis, checkOutMillis } = req.body;
  setData({
    logs: getData().logs.map(l => l.id === id ? { ...l, checkInMillis, checkOutMillis } : l),
  });
  res.json({ ok: true });
});

// ═══════════ LEAVES ═══════════

app.get("/api/leaves", (req, res) => {
  const { employeeId } = req.query;
  let leaves = getData().leaves;
  if (employeeId) leaves = leaves.filter(l => l.employeeId === employeeId);
  leaves.sort((a, b) => b.requestedAtMillis - a.requestedAtMillis);
  res.json(leaves);
});

app.post("/api/leaves", (req, res) => {
  const { employeeId, startMillis, endMillis, reason, type } = req.body;
  const emp = getData().employees.find(e => e.employeeId === employeeId);
  if (!emp) return res.status(404).json({ ok: false, error: "Employee not found" });
  const leave: LeaveRequest = {
    id: generateId("lv"),
    employeeId, employeeName: emp.name,
    startMillis, endMillis, reason, type,
    status: "PENDING", payType: null,
    requestedAtMillis: Date.now(),
  };
  setData({ leaves: [leave, ...getData().leaves] });
  res.json({ ok: true, leave });
});

app.put("/api/leaves/:id", (req, res) => {
  const { id } = req.params;
  const { status, payType } = req.body as { status: LeaveStatus; payType: LeavePayType | null };
  const leaves = getData().leaves.map(l => {
    if (l.id !== id) return l;
    const updated = { ...l, status, payType: status === "APPROVED" ? payType : null };
    if (status === "APPROVED" && Date.now() >= l.startMillis && Date.now() <= l.endMillis) {
      const employees = getData().employees.map(e =>
        e.id === l.employeeId ? { ...e, status: "ON_LEAVE" as const } : e
      );
      setData({ employees });
    }
    return updated;
  });
  setData({ leaves });
  res.json({ ok: true });
});

// ═══════════ SALARY ═══════════

app.get("/api/salary/:employeeId", (req, res) => {
  const emp = getData().employees.find(e => e.id === req.params.employeeId);
  if (!emp) return res.status(404).json({ ok: false });
  const now = new Date();
  res.json(salaryFor(emp, now.getMonth(), now.getFullYear()));
});

app.get("/api/salary/:employeeId/:month/:year", (req, res) => {
  const emp = getData().employees.find(e => e.id === req.params.employeeId);
  if (!emp) return res.status(404).json({ ok: false });
  res.json(salaryFor(emp, parseInt(req.params.month), parseInt(req.params.year)));
});

app.get("/api/salaries/:month/:year", (req, res) => {
  const m = parseInt(req.params.month), y = parseInt(req.params.year);
  res.json(getData().employees.map(e => salaryFor(e, m, y)));
});

app.get("/api/summaries/:year", (req, res) => {
  const year = parseInt(req.params.year);
  const summaries: YearlySummary[] = getData().employees.map(emp => {
    const monthly = Array.from({ length: 12 }, (_, m) => salaryFor(emp, m, year).netPayable);
    return {
      employeeId: emp.id, employeeName: emp.name, baseSalary: emp.baseSalary,
      monthlyNet: monthly, totalNet: monthly.reduce((a, b) => a + b, 0),
    };
  });
  res.json(summaries);
});

// ═══════════ WORKSITE ═══════════

app.get("/api/worksite", (_req, res) => {
  res.json(getData().workSite);
});

app.put("/api/worksite", (req, res) => {
  const { name, lat, lon, radius } = req.body;
  setData({
    workSite: {
      name: name || getData().workSite.name,
      center: { latitude: lat ?? getData().workSite.center.latitude, longitude: lon ?? getData().workSite.center.longitude },
      radiusMeters: radius ?? getData().workSite.radiusMeters,
    },
  });
  res.json({ ok: true, workSite: getData().workSite });
});

// ═══════════ CSV EXPORT ═══════════

app.get("/api/export/payroll/:month/:year", (req, res) => {
  const m = parseInt(req.params.month), y = parseInt(req.params.year);
  const rows = getData().employees.map(e => salaryFor(e, m, y));
  const csv = ["Employee,Base Salary,Working Days,Days Worked,Paid Leave,Unpaid Leave,Absent,Per Day,Gross,Deductions,Net Payable"];
  rows.forEach(r => {
    csv.push(`${r.employeeName},${r.baseSalary},${r.totalWorkingDays},${r.daysWorked},${r.paidLeaveDays},${r.unpaidLeaveDays},${r.absentDays},${r.perDayRate},${r.grossEarned},${r.deductions},${r.netPayable}`);
  });
  res.setHeader("Content-Type", "text/csv");
  res.setHeader("Content-Disposition", `attachment; filename=payroll_${y}_${m + 1}.csv`);
  res.send(csv.join("\n"));
});

app.get("/api/export/summary/:year", (req, res) => {
  const year = parseInt(req.params.year);
  const summaries: YearlySummary[] = getData().employees.map(emp => {
    const monthly = Array.from({ length: 12 }, (_, m) => salaryFor(emp, m, year).netPayable);
    return { employeeId: emp.id, employeeName: emp.name, baseSalary: emp.baseSalary, monthlyNet: monthly, totalNet: monthly.reduce((a, b) => a + b, 0) };
  });
  const csv = ["Employee,Base Salary," + Array.from({ length: 12 }, (_, i) => new Date(0, i).toLocaleString("en", { month: "short" })).join(",") + ",Total"];
  summaries.forEach(s => {
    csv.push(`${s.employeeName},${s.baseSalary},${s.monthlyNet.join(",")},${s.totalNet}`);
  });
  res.setHeader("Content-Type", "text/csv");
  res.setHeader("Content-Disposition", `attachment; filename=yearly_summary_${year}.csv`);
  res.send(csv.join("\n"));
});

app.get("/api/export/attendance/:month/:year", (req, res) => {
  const m = parseInt(req.params.month), y = parseInt(req.params.year);
  const logs = getData().logs.filter(l => inMonth(l.checkInMillis, m, y)).sort((a, b) => b.checkInMillis - a.checkInMillis);
  const empMap = new Map(getData().employees.map(e => [e.id, e]));
  const csv = ["Log ID,Employee,Date,Check In,Check Out,Duration (h),Distance (m),Verified"];
  logs.forEach(l => {
    const emp = empMap.get(l.employeeId);
    const dur = l.checkOutMillis ? ((l.checkOutMillis - l.checkInMillis) / 3_600_000).toFixed(2) : "active";
    csv.push(`${l.id},${emp?.name || l.employeeId},${new Date(l.checkInMillis).toLocaleDateString()},${new Date(l.checkInMillis).toLocaleTimeString()},${l.checkOutMillis ? new Date(l.checkOutMillis).toLocaleTimeString() : "—"},${dur},${l.distanceFromSite.toFixed(1)},${l.verified}`);
  });
  res.setHeader("Content-Type", "text/csv");
  res.setHeader("Content-Disposition", `attachment; filename=attendance_${y}_${m + 1}.csv`);
  res.send(csv.join("\n"));
});

// ── Haversine ──
function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6_371_000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

app.listen(PORT, "0.0.0.0", () => {
  console.log(`\n🦁 Med Lion HR Server running on http://0.0.0.0:${PORT}`);
  console.log(`   Local:   http://localhost:${PORT}`);
  console.log(`   Network: http://<your-ip>:${PORT}\n`);
});
