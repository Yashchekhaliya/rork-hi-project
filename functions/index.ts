/**
 * Med Lion HR — Cloudflare Worker API
 *
 * All endpoints are backed by Supabase Postgres. The Worker handles auth
 * (admin userId+password, employee password verification), geofence checks,
 * salary computation, and CSV export.
 *
 * Reachable at: https://li980wrgnunptwig2nzqh-backend.rork.app/<path>
 */

import { createClient, SupabaseClient } from "@supabase/supabase-js";

// ---------------------------------------------------------------------------
// Types (mirror server/src/types.ts)
// ---------------------------------------------------------------------------

interface GeoPoint {
  latitude: number;
  longitude: number;
}

interface WorkSite {
  name: string;
  center: GeoPoint;
  radiusMeters: number;
}

interface Employee {
  id: string;
  employeeId: string;
  password: string;
  name: string;
  role: string;
  avatarColor: number;
  baseSalary: number;
  status: "CHECKED_IN" | "CHECKED_OUT" | "ON_LEAVE";
}

interface AttendanceLog {
  id: string;
  employeeId: string;
  checkInMillis: number;
  checkOutMillis: number | null;
  location: GeoPoint;
  distanceFromSite: number;
  verified: boolean;
}

interface LeaveRequest {
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

interface SalaryBreakdown {
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

interface YearlySummary {
  employeeId: string;
  employeeName: string;
  baseSalary: number;
  monthlyNet: number[];
  totalNet: number;
}

// ---------------------------------------------------------------------------
// CORS
// ---------------------------------------------------------------------------

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
};

function ok(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function err(message: string, status = 400): Response {
  return new Response(JSON.stringify({ ok: false, error: message }), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

// ---------------------------------------------------------------------------
// Supabase admin client (service role — bypasses RLS)
// ---------------------------------------------------------------------------

function getAdminClient(env: Record<string, string>): SupabaseClient {
  // Use the Rork-managed Supabase; the tables are provisioned there.
  // Prefer EXPO_PUBLIC_* (Rork-managed) over bare keys (custom user project)
  // so the Worker always hits the database with the correct schema.
  const url = env.EXPO_PUBLIC_SUPABASE_URL || env.SUPABASE_URL || "";
  const key = env.EXPO_PUBLIC_SUPABASE_ANON_KEY || env.SUPABASE_ANON_KEY || env.SUPABASE_SERVICE_ROLE_KEY || "";
  if (!url || !key) {
    const available = Object.keys(env).filter(k => k.includes("SUPA") || k.includes("supa"));
    throw new Error(`Missing Supabase env vars. Available Supa keys: [${available.join(", ")}]`);
  }
  return createClient(url, key);
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const DAILY_WORKING_HOURS = 8.0;

function generateId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
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

function inMonth(millis: number, month: number, year: number): boolean {
  const d = new Date(millis);
  return d.getMonth() === month && d.getFullYear() === year;
}

function dayKey(millis: number): number {
  const d = new Date(millis);
  return d.getFullYear() * 1000 + Math.floor((d.getTime() - new Date(d.getFullYear(), 0, 0).getTime()) / 86400000);
}

function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6_371_000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function leaveDays(l: LeaveRequest): number {
  return Math.max(1, Math.floor((l.endMillis - l.startMillis) / 86_400_000) + 1);
}

function computeSalary(
  emp: Employee,
  logs: AttendanceLog[],
  leaves: LeaveRequest[],
  month: number,
  year: number,
): SalaryBreakdown {
  const totalDays = standardWorkingDays(month, year);
  const perDay = totalDays > 0 ? emp.baseSalary / totalDays : 0;

  const empLogs = logs.filter((l) => l.employeeId === emp.id && inMonth(l.checkInMillis, month, year));
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

  const approved = leaves.filter(
    (l) => l.employeeId === emp.id && l.status === "APPROVED" && inMonth(l.startMillis, month, year),
  );
  const paidLeaveDays = approved.filter((l) => l.payType === "WITH_PAY").reduce((s, l) => s + leaveDays(l), 0);
  const unpaidLeaveDays = approved.filter((l) => l.payType === "WITHOUT_PAY").reduce((s, l) => s + leaveDays(l), 0);

  const accountedDays = fractionalDays + paidLeaveDays + unpaidLeaveDays;
  const absentDays = Math.max(0, totalDays - accountedDays);
  const gross = perDay * (fractionalDays + paidLeaveDays);

  return {
    employeeId: emp.id,
    employeeName: emp.name,
    baseSalary: emp.baseSalary,
    totalWorkingDays: totalDays,
    daysWorked: Math.round(fractionalDays * 100) / 100,
    paidLeaveDays,
    unpaidLeaveDays,
    absentDays: Math.round(absentDays * 100) / 100,
    perDayRate: Math.round(perDay * 100) / 100,
    grossEarned: Math.round(gross * 100) / 100,
    deductions: Math.round(perDay * (unpaidLeaveDays + absentDays) * 100) / 100,
    netPayable: Math.round(gross * 100) / 100,
    dailyWorkingHours: DAILY_WORKING_HOURS,
    month,
    year,
  };
}

// ---------------------------------------------------------------------------
// Route helpers — extract path params from URL pattern
// ---------------------------------------------------------------------------

function matchPath(pathname: string, pattern: string): Record<string, string> | null {
  const pathParts = pathname.split("/").filter(Boolean);
  const patParts = pattern.split("/").filter(Boolean);
  if (pathParts.length !== patParts.length) return null;
  const params: Record<string, string> = {};
  for (let i = 0; i < patParts.length; i++) {
    if (patParts[i].startsWith(":")) {
      params[patParts[i].slice(1)] = pathParts[i];
    } else if (patParts[i] !== pathParts[i]) {
      return null;
    }
  }
  return params;
}

// ---------------------------------------------------------------------------
// Main fetch handler
// ---------------------------------------------------------------------------

export default {
  async fetch(request: Request, env: Record<string, string>): Promise<Response> {
    // CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const p = url.pathname;
    const m = request.method;

    try {
      const supabase = getAdminClient(env);

      // ── Root ──
      if (p === "/" && m === "GET") {
        return ok({
          name: "Med Lion HR Server (Cloudflare + Supabase)",
          version: "2.1.3",
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
      }

      // ── Health ──
      if (p === "/api/health" && m === "GET") {
        return ok({ ok: true, name: "Med Lion HR Server", time: Date.now() });
      }

      // ── Admin Auth ──
      if (p === "/api/admin/login" && m === "POST") {
        const { userId, password } = await request.json() as { userId: string; password: string };
        if (!userId || !password) return err("User ID and password are required");

        const { data: uidRow, error: uidErr } = await supabase.from("admin_settings").select("value").eq("key", "admin_user_id").maybeSingle();
        if (uidErr) return err(`Database error: ${uidErr.message}`, 500);
        if (!uidRow || uidRow.value !== userId) return err("Invalid credentials", 401);

        const { data: pwdRow, error: pwdErr } = await supabase.from("admin_settings").select("value").eq("key", "admin_password").maybeSingle();
        if (pwdErr) return err(`Database error: ${pwdErr.message}`, 500);
        if (!pwdRow || pwdRow.value !== password) return err("Invalid credentials", 401);

        return ok({ ok: true, token: "admin-session" });
      }

      if (p === "/api/admin/change-password" && m === "POST") {
        const { password } = await request.json() as { password: string };
        if (!password || password.length < 4) return err("Password must be at least 4 characters");
        const { error: upErr } = await supabase.from("admin_settings").upsert({ key: "admin_password", value: password }, { onConflict: "key" });
        if (upErr) return err(`Database error: ${upErr.message}`, 500);
        return ok({ ok: true });
      }

      // ── Employees ──
      if (p === "/api/employees" && m === "GET") {
        const { data: rows } = await supabase.from("employees").select("*").order("name");
        const employees: Employee[] = (rows || []).map(rowToEmployee);
        return ok(employees);
      }

      if (p === "/api/employees" && m === "POST") {
        const body = await request.json() as {
          employeeId: string; password: string; name: string; role: string; baseSalary: number;
        };
        if (!body.employeeId || !body.password || !body.name || body.password.length < 4) {
          return err("Invalid employee data");
        }
        const { data: existing } = await supabase.from("employees").select("id").eq("employee_id", body.employeeId).maybeSingle();
        if (existing) return err("Employee ID already exists", 409);

        const colors = [0xFF26E8FF, 0xFF9D5BFF, 0xFFFF4ECB, 0xFF5BFFB0, 0xFFFFC24B, 0xFF3B82F6, 0xFFE040FB];
        const emp = {
          id: generateId("emp"),
          employee_id: body.employeeId,
          password: body.password,
          name: body.name,
          role: body.role || "Staff",
          avatar_color: colors[Math.floor(Math.random() * colors.length)],
          base_salary: body.baseSalary || 0,
          status: "CHECKED_OUT",
        };
        await supabase.from("employees").insert(emp);
        return ok({ ok: true, employee: rowToEmployee(emp) });
      }

      if (m === "DELETE" && matchPath(p, "api/employees/:id")) {
        const params = matchPath(p, "api/employees/:id")!;
        const { data: emp } = await supabase.from("employees").select("id").eq("id", params.id).maybeSingle();
        if (!emp) return err("Employee not found", 404);
        await supabase.from("employees").delete().eq("id", params.id);
        await supabase.from("attendance_logs").delete().eq("employee_id", params.id);
        await supabase.from("leave_requests").delete().eq("employee_id", params.id);
        return ok({ ok: true });
      }

      if (p === "/api/employees/login" && m === "POST") {
        const { employeeId, password } = await request.json() as { employeeId: string; password: string };
        const { data } = await supabase.from("employees").select("*").eq("employee_id", employeeId).maybeSingle();
        if (!data || data.password !== password) return err("Invalid credentials", 401);
        return ok({ ok: true, employee: rowToEmployee(data) });
      }

      if (m === "POST" && matchPath(p, "api/employees/:id/change-password")) {
        const params = matchPath(p, "api/employees/:id/change-password")!;
        const { currentPassword, newPassword } = await request.json() as { currentPassword: string; newPassword: string };
        if (!newPassword || newPassword.length < 4) return err("New password too short");
        const { data: emp } = await supabase.from("employees").select("*").eq("id", params.id).maybeSingle();
        if (!emp || emp.password !== currentPassword) return err("Invalid current password", 401);
        await supabase.from("employees").update({ password: newPassword }).eq("id", params.id);
        return ok({ ok: true });
      }

      // ── Attendance ──
      if (p === "/api/attendance" && m === "GET") {
        const employeeId = url.searchParams.get("employeeId");
        const month = url.searchParams.get("month");
        const year = url.searchParams.get("year");

        let query = supabase.from("attendance_logs").select("*");
        if (employeeId) query = query.eq("employee_id", employeeId);
        const { data: rows } = await query.order("check_in_millis", { ascending: false });

        let logs: AttendanceLog[] = (rows || []).map(rowToLog);
        if (month !== null && year !== null) {
          const mNum = parseInt(month), yNum = parseInt(year);
          logs = logs.filter((l) => inMonth(l.checkInMillis, mNum, yNum));
        }
        return ok(logs);
      }

      if (p === "/api/attendance/checkin" && m === "POST") {
        const { employeeId, location } = await request.json() as { employeeId: string; location: GeoPoint };

        const { data: openLog } = await supabase.from("attendance_logs").select("*")
          .eq("employee_id", employeeId).is("check_out_millis", null).maybeSingle();
        if (openLog) return ok({ ok: true, log: rowToLog(openLog), message: "Already checked in" });

        const { data: ws } = await supabase.from("work_site").select("*").single();
        const workSite: WorkSite = ws ? {
          name: ws.name,
          center: { latitude: ws.center_lat, longitude: ws.center_lon },
          radiusMeters: ws.radius_meters,
        } : { name: "Default", center: { latitude: 0, longitude: 0 }, radiusMeters: 1000 };

        const dist = haversine(workSite.center.latitude, workSite.center.longitude, location.latitude, location.longitude);
        if (dist > workSite.radiusMeters) {
          return err("Outside geofence", 403);
        }

        const logRow = {
          id: generateId("log"),
          employee_id: employeeId,
          check_in_millis: Date.now(),
          check_out_millis: null,
          location_lat: location.latitude,
          location_lon: location.longitude,
          distance_from_site: dist,
          verified: true,
        };
        await supabase.from("attendance_logs").insert(logRow);
        await supabase.from("employees").update({ status: "CHECKED_IN" }).eq("id", employeeId);
        return ok({ ok: true, log: rowToLog(logRow) });
      }

      if (p === "/api/attendance/checkout" && m === "POST") {
        const { employeeId } = await request.json() as { employeeId: string };
        const { data: openLog } = await supabase.from("attendance_logs").select("*")
          .eq("employee_id", employeeId).is("check_out_millis", null).maybeSingle();
        if (!openLog) return err("No open session");

        await supabase.from("attendance_logs").update({ check_out_millis: Date.now() }).eq("id", openLog.id);
        await supabase.from("employees").update({ status: "CHECKED_OUT" }).eq("id", employeeId);

        const { data: updated } = await supabase.from("attendance_logs").select("*").eq("id", openLog.id).single();
        return ok({ ok: true, log: rowToLog(updated!) });
      }

      if (m === "PUT" && matchPath(p, "api/attendance/:id")) {
        const params = matchPath(p, "api/attendance/:id")!;
        const { checkInMillis, checkOutMillis } = await request.json() as { checkInMillis: number; checkOutMillis: number | null };
        await supabase.from("attendance_logs").update({
          check_in_millis: checkInMillis,
          check_out_millis: checkOutMillis,
        }).eq("id", params.id);
        return ok({ ok: true });
      }

      // ── Leaves ──
      if (p === "/api/leaves" && m === "GET") {
        const employeeId = url.searchParams.get("employeeId");
        let query = supabase.from("leave_requests").select("*");
        if (employeeId) query = query.eq("employee_id", employeeId);
        const { data: rows } = await query.order("requested_at_millis", { ascending: false });
        return ok((rows || []).map(rowToLeave));
      }

      if (p === "/api/leaves" && m === "POST") {
        const { employeeId, startMillis, endMillis, reason, type } = await request.json() as {
          employeeId: string; startMillis: number; endMillis: number; reason: string; type: string;
        };
        const { data: emp } = await supabase.from("employees").select("*").eq("id", employeeId).maybeSingle();
        if (!emp) return err("Employee not found", 404);

        const leaveRow = {
          id: generateId("lv"),
          employee_id: employeeId,
          employee_name: emp.name,
          start_millis: startMillis,
          end_millis: endMillis,
          reason,
          type,
          status: "PENDING",
          pay_type: null,
          requested_at_millis: Date.now(),
        };
        await supabase.from("leave_requests").insert(leaveRow);
        return ok({ ok: true, leave: rowToLeave(leaveRow) });
      }

      if (m === "PUT" && matchPath(p, "api/leaves/:id")) {
        const params = matchPath(p, "api/leaves/:id")!;
        const { status, payType } = await request.json() as {
          status: "PENDING" | "APPROVED" | "REJECTED";
          payType: "WITH_PAY" | "WITHOUT_PAY" | null;
        };
        await supabase.from("leave_requests").update({
          status,
          pay_type: status === "APPROVED" ? payType : null,
        }).eq("id", params.id);

        if (status === "APPROVED") {
          const { data: lv } = await supabase.from("leave_requests").select("*").eq("id", params.id).single();
          if (lv) {
            const now = Date.now();
            if (now >= lv.start_millis && now <= lv.end_millis) {
              await supabase.from("employees").update({ status: "ON_LEAVE" }).eq("id", lv.employee_id);
            }
          }
        }
        return ok({ ok: true });
      }

      // ── Salary ──
      if (m === "GET" && matchPath(p, "api/salary/:employeeId")) {
        const params = matchPath(p, "api/salary/:employeeId")!;
        const { data: emp } = await supabase.from("employees").select("*").eq("id", params.employeeId).maybeSingle();
        if (!emp) return err("Not found", 404);

        const { data: allLogs } = await supabase.from("attendance_logs").select("*");
        const { data: allLeaves } = await supabase.from("leave_requests").select("*");
        const now = new Date();
        return ok(computeSalary(rowToEmployee(emp), (allLogs || []).map(rowToLog), (allLeaves || []).map(rowToLeave), now.getMonth(), now.getFullYear()));
      }

      if (m === "GET" && matchPath(p, "api/salary/:employeeId/:month/:year")) {
        const params = matchPath(p, "api/salary/:employeeId/:month/:year")!;
        const { data: emp } = await supabase.from("employees").select("*").eq("id", params.employeeId).maybeSingle();
        if (!emp) return err("Not found", 404);

        const { data: allLogs } = await supabase.from("attendance_logs").select("*");
        const { data: allLeaves } = await supabase.from("leave_requests").select("*");
        return ok(computeSalary(rowToEmployee(emp), (allLogs || []).map(rowToLog), (allLeaves || []).map(rowToLeave), parseInt(params.month), parseInt(params.year)));
      }

      if (m === "GET" && matchPath(p, "api/salaries/:month/:year")) {
        const params = matchPath(p, "api/salaries/:month/:year")!;
        const { data: emps } = await supabase.from("employees").select("*");
        const { data: allLogs } = await supabase.from("attendance_logs").select("*");
        const { data: allLeaves } = await supabase.from("leave_requests").select("*");
        const logs = (allLogs || []).map(rowToLog);
        const leaves = (allLeaves || []).map(rowToLeave);
        const mNum = parseInt(params.month), yNum = parseInt(params.year);
        return ok((emps || []).map((e) => computeSalary(rowToEmployee(e), logs, leaves, mNum, yNum)));
      }

      if (m === "GET" && matchPath(p, "api/summaries/:year")) {
        const params = matchPath(p, "api/summaries/:year")!;
        const year = parseInt(params.year);
        const { data: emps } = await supabase.from("employees").select("*");
        const { data: allLogs } = await supabase.from("attendance_logs").select("*");
        const { data: allLeaves } = await supabase.from("leave_requests").select("*");
        const logs = (allLogs || []).map(rowToLog);
        const leaves = (allLeaves || []).map(rowToLeave);

        const summaries: YearlySummary[] = (emps || []).map((emp) => {
          const e = rowToEmployee(emp);
          const monthly = Array.from({ length: 12 }, (_, m) => computeSalary(e, logs, leaves, m, year).netPayable);
          return {
            employeeId: e.id,
            employeeName: e.name,
            baseSalary: e.baseSalary,
            monthlyNet: monthly,
            totalNet: monthly.reduce((a, b) => a + b, 0),
          };
        });
        return ok(summaries);
      }

      // ── Worksite ──
      if (p === "/api/worksite" && m === "GET") {
        const { data } = await supabase.from("work_site").select("*").single();
        return ok(data ? {
          name: data.name,
          center: { latitude: data.center_lat, longitude: data.center_lon },
          radiusMeters: data.radius_meters,
        } : { name: "Default", center: { latitude: 0, longitude: 0 }, radiusMeters: 1000 });
      }

      if (p === "/api/worksite" && m === "PUT") {
        const { name, lat, lon, radius } = await request.json() as { name: string; lat: number; lon: number; radius: number };
        const updated = {
          name: name || "Sonorous — Vapi",
          center_lat: lat ?? 20.3734,
          center_lon: lon ?? 72.9141,
          radius_meters: radius ?? 1000,
        };
        await supabase.from("work_site").upsert({ id: 1, ...updated }, { onConflict: "id" });
        const { data } = await supabase.from("work_site").select("*").single();
        return ok({ ok: true, workSite: data ? {
          name: data.name,
          center: { latitude: data.center_lat, longitude: data.center_lon },
          radiusMeters: data.radius_meters,
        } : null });
      }

      // ── CSV Exports ──
      if (m === "GET" && matchPath(p, "api/export/payroll/:month/:year")) {
        const params = matchPath(p, "api/export/payroll/:month/:year")!;
        const { data: emps } = await supabase.from("employees").select("*");
        const { data: allLogs } = await supabase.from("attendance_logs").select("*");
        const { data: allLeaves } = await supabase.from("leave_requests").select("*");
        const logs = (allLogs || []).map(rowToLog);
        const leaves = (allLeaves || []).map(rowToLeave);
        const mNum = parseInt(params.month), yNum = parseInt(params.year);
        const rows = (emps || []).map((e) => computeSalary(rowToEmployee(e), logs, leaves, mNum, yNum));

        const csv = ["Employee,Base Salary,Working Days,Days Worked,Paid Leave,Unpaid Leave,Absent,Per Day,Gross,Deductions,Net Payable"];
        rows.forEach((r) => {
          csv.push(`${r.employeeName},${r.baseSalary},${r.totalWorkingDays},${r.daysWorked},${r.paidLeaveDays},${r.unpaidLeaveDays},${r.absentDays},${r.perDayRate},${r.grossEarned},${r.deductions},${r.netPayable}`);
        });
        return new Response(csv.join("\n"), {
          headers: { ...corsHeaders, "Content-Type": "text/csv", "Content-Disposition": `attachment; filename=payroll_${yNum}_${mNum + 1}.csv` },
        });
      }

      if (m === "GET" && matchPath(p, "api/export/summary/:year")) {
        const params = matchPath(p, "api/export/summary/:year")!;
        const year = parseInt(params.year);
        const { data: emps } = await supabase.from("employees").select("*");
        const { data: allLogs } = await supabase.from("attendance_logs").select("*");
        const { data: allLeaves } = await supabase.from("leave_requests").select("*");
        const logs = (allLogs || []).map(rowToLog);
        const leaves = (allLeaves || []).map(rowToLeave);

        const monthNames = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
        const csv = ["Employee,Base Salary," + monthNames.join(",") + ",Total"];
        (emps || []).forEach((emp) => {
          const e = rowToEmployee(emp);
          const monthly = Array.from({ length: 12 }, (_, m) => computeSalary(e, logs, leaves, m, year).netPayable);
          const total = monthly.reduce((a, b) => a + b, 0);
          csv.push(`${e.name},${e.baseSalary},${monthly.join(",")},${total}`);
        });
        return new Response(csv.join("\n"), {
          headers: { ...corsHeaders, "Content-Type": "text/csv", "Content-Disposition": `attachment; filename=yearly_summary_${year}.csv` },
        });
      }

      if (m === "GET" && matchPath(p, "api/export/attendance/:month/:year")) {
        const params = matchPath(p, "api/export/attendance/:month/:year")!;
        const mNum = parseInt(params.month), yNum = parseInt(params.year);
        const { data: allLogs } = await supabase.from("attendance_logs").select("*").order("check_in_millis", { ascending: false });
        const { data: emps } = await supabase.from("employees").select("*");
        const empMap = new Map((emps || []).map((e) => [e.id, e.name]));

        const logs = (allLogs || []).map(rowToLog).filter((l) => inMonth(l.checkInMillis, mNum, yNum));
        const csv = ["Log ID,Employee,Date,Check In,Check Out,Duration (h),Distance (m),Verified"];
        logs.forEach((l) => {
          const dur = l.checkOutMillis ? ((l.checkOutMillis - l.checkInMillis) / 3_600_000).toFixed(2) : "active";
          csv.push(`${l.id},${empMap.get(l.employeeId) || l.employeeId},${new Date(l.checkInMillis).toLocaleDateString()},${new Date(l.checkInMillis).toLocaleTimeString()},${l.checkOutMillis ? new Date(l.checkOutMillis).toLocaleTimeString() : "—"},${dur},${l.distanceFromSite.toFixed(1)},${l.verified}`);
        });
        return new Response(csv.join("\n"), {
          headers: { ...corsHeaders, "Content-Type": "text/csv", "Content-Disposition": `attachment; filename=attendance_${yNum}_${mNum + 1}.csv` },
        });
      }

      // ── Setup — create tables on first run ──
      if (p === "/api/setup" && m === "POST") {
        const results: string[] = [];

        // Create admin_settings table
        const { error: e1 } = await supabase.from("admin_settings").select("key").limit(0);
        if (e1) {
          const { error: ce1 } = await supabase.rpc("create_admin_settings_table");
          if (ce1) results.push(`admin_settings: ${ce1.message}`);
          else results.push("admin_settings: created");
        } else {
          results.push("admin_settings: exists");
        }

        // Create work_site table
        const { error: e2 } = await supabase.from("work_site").select("id").limit(0);
        if (e2) {
          const { error: ce2 } = await supabase.rpc("create_work_site_table");
          if (ce2) results.push(`work_site: ${ce2.message}`);
          else results.push("work_site: created");
        } else {
          results.push("work_site: exists");
        }

        // Create employees table
        const { error: e3 } = await supabase.from("employees").select("id").limit(0);
        if (e3) {
          const { error: ce3 } = await supabase.rpc("create_employees_table");
          if (ce3) results.push(`employees: ${ce3.message}`);
          else results.push("employees: created");
        } else {
          results.push("employees: exists");
        }

        // Create attendance_logs table
        const { error: e4 } = await supabase.from("attendance_logs").select("id").limit(0);
        if (e4) {
          const { error: ce4 } = await supabase.rpc("create_attendance_logs_table");
          if (ce4) results.push(`attendance_logs: ${ce4.message}`);
          else results.push("attendance_logs: created");
        } else {
          results.push("attendance_logs: exists");
        }

        // Create leave_requests table
        const { error: e5 } = await supabase.from("leave_requests").select("id").limit(0);
        if (e5) {
          const { error: ce5 } = await supabase.rpc("create_leave_requests_table");
          if (ce5) results.push(`leave_requests: ${ce5.message}`);
          else results.push("leave_requests: created");
        } else {
          results.push("leave_requests: exists");
        }

        // Seed admin credentials if missing
        const { data: existingUid } = await supabase.from("admin_settings").select("value").eq("key", "admin_user_id").maybeSingle();
        if (!existingUid) {
          await supabase.from("admin_settings").upsert({ key: "admin_user_id", value: "admin" }, { onConflict: "key" });
          results.push("admin_user_id: seeded");
        } else {
          results.push("admin_user_id: exists");
        }

        const { data: existingPwd } = await supabase.from("admin_settings").select("value").eq("key", "admin_password").maybeSingle();
        if (!existingPwd) {
          await supabase.from("admin_settings").upsert({ key: "admin_password", value: "Yashwant@2000" }, { onConflict: "key" });
          results.push("admin_password: seeded");
        } else {
          results.push("admin_password: exists");
        }

        // Seed worksite if missing
        const { data: existingWs } = await supabase.from("work_site").select("id").maybeSingle();
        if (!existingWs) {
          await supabase.from("work_site").upsert({ id: 1, name: "Sonorous — Vapi", center_lat: 20.3734, center_lon: 72.9141, radius_meters: 1000 }, { onConflict: "id" });
          results.push("work_site: seeded");
        } else {
          results.push("work_site: exists");
        }

        return ok({ ok: true, results });
      }

      // ── 404 ──
      return err("Not found", 404);
    } catch (e: unknown) {
      console.error(e);
      return err(e instanceof Error ? e.message : "Internal server error", 500);
    }
  },
};

// ---------------------------------------------------------------------------
// Row mappers — convert Supabase snake_case rows to camelCase app types
// ---------------------------------------------------------------------------

function rowToEmployee(row: Record<string, unknown>): Employee {
  return {
    id: row.id as string,
    employeeId: row.employee_id as string,
    password: row.password as string,
    name: row.name as string,
    role: row.role as string,
    avatarColor: row.avatar_color as number,
    baseSalary: row.base_salary as number,
    status: row.status as Employee["status"],
  };
}

function rowToLog(row: Record<string, unknown>): AttendanceLog {
  return {
    id: row.id as string,
    employeeId: row.employee_id as string,
    checkInMillis: row.check_in_millis as number,
    checkOutMillis: (row.check_out_millis as number) || null,
    location: { latitude: row.location_lat as number, longitude: row.location_lon as number },
    distanceFromSite: row.distance_from_site as number,
    verified: row.verified as boolean,
  };
}

function rowToLeave(row: Record<string, unknown>): LeaveRequest {
  return {
    id: row.id as string,
    employeeId: row.employee_id as string,
    employeeName: row.employee_name as string,
    startMillis: row.start_millis as number,
    endMillis: row.end_millis as number,
    reason: row.reason as string,
    type: row.type as string,
    status: row.status as LeaveRequest["status"],
    payType: (row.pay_type as LeaveRequest["payType"]) || null,
    requestedAtMillis: row.requested_at_millis as number,
  };
}
