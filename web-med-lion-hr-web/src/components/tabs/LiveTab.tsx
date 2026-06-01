import { useQuery } from "@tanstack/react-query";
import { MapPin, Clock, Users, CheckCircle2, Plane } from "lucide-react";

import { api, type Employee, type AttendanceLog } from "@/lib/api";
import { cn } from "@/lib/utils";

function statusMeta(status: Employee["status"]) {
  switch (status) {
    case "CHECKED_IN":
      return { label: "On site", color: "text-emerald-400", dot: "bg-emerald-400", icon: CheckCircle2 };
    case "ON_LEAVE":
      return { label: "On leave", color: "text-amber-400", dot: "bg-amber-400", icon: Plane };
    default:
      return { label: "Off", color: "text-muted-foreground", dot: "bg-muted-foreground", icon: Clock };
  }
}

function durationStr(log: AttendanceLog): string {
  const ms = (log.checkOutMillis ?? Date.now()) - log.checkInMillis;
  const h = Math.floor(ms / 3_600_000);
  const m = Math.floor((ms % 3_600_000) / 60_000);
  return `${h}h ${m}m`;
}

const LiveTab = () => {
  const { data: employees = [] } = useQuery({ queryKey: ["employees"], queryFn: api.employees });
  const { data: logs = [] } = useQuery({ queryKey: ["attendance"], queryFn: () => api.attendance() });
  const { data: worksite } = useQuery({ queryKey: ["worksite"], queryFn: api.worksite });

  const onSite = employees.filter((e) => e.status === "CHECKED_IN").length;
  const onLeave = employees.filter((e) => e.status === "ON_LEAVE").length;

  const openLogFor = (id: string) => logs.find((l) => l.employeeId === id && !l.checkOutMillis);

  return (
    <div className="space-y-6">
      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard icon={Users} label="Team" value={employees.length} accent="text-primary" />
        <StatCard icon={CheckCircle2} label="On site" value={onSite} accent="text-emerald-400" />
        <StatCard icon={Plane} label="On leave" value={onLeave} accent="text-amber-400" />
        <StatCard icon={Clock} label="Off" value={employees.length - onSite - onLeave} accent="text-muted-foreground" />
      </div>

      {/* Worksite */}
      {worksite && (
        <div className="glass flex items-center gap-3 p-5">
          <div className="glass flex h-11 w-11 items-center justify-center rounded-xl border-primary/30">
            <MapPin className="h-5 w-5 text-primary" />
          </div>
          <div>
            <p className="font-semibold">{worksite.name}</p>
            <p className="text-sm text-muted-foreground">
              Geofence {Math.round(worksite.radiusMeters)}m · {worksite.center.latitude.toFixed(4)}, {worksite.center.longitude.toFixed(4)}
            </p>
          </div>
        </div>
      )}

      {/* Employee list */}
      <div>
        <h2 className="mb-3 text-lg font-semibold">Live presence</h2>
        <div className="space-y-3">
          {employees.map((emp) => {
            const meta = statusMeta(emp.status);
            const open = openLogFor(emp.id);
            const Icon = meta.icon;
            const color = `#${(emp.avatarColor & 0xffffff).toString(16).padStart(6, "0")}`;
            return (
              <div key={emp.id} className="glass glass-hover flex items-center gap-4 p-4">
                <div
                  className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-full text-sm font-bold"
                  style={{ background: `${color}33`, color }}
                >
                  {emp.name.charAt(0)}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{emp.name}</p>
                  <p className="truncate text-xs text-muted-foreground">{emp.role} · {emp.employeeId}</p>
                </div>
                {open && (
                  <div className="hidden text-right sm:block">
                    <p className="text-sm font-semibold text-emerald-400">{durationStr(open)}</p>
                    <p className="text-xs text-muted-foreground">{Math.round(open.distanceFromSite)}m away</p>
                  </div>
                )}
                <div className={cn("flex items-center gap-2", meta.color)}>
                  <span className={cn("h-2.5 w-2.5 rounded-full animate-pulse-dot", meta.dot)} />
                  <Icon className="h-4 w-4" />
                  <span className="hidden text-sm font-medium md:inline">{meta.label}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

const StatCard = ({ icon: Icon, label, value, accent }: { icon: typeof Users; label: string; value: number; accent: string }) => (
  <div className="glass p-5">
    <Icon className={cn("mb-3 h-5 w-5", accent)} />
    <p className="text-3xl font-bold tracking-tight">{value}</p>
    <p className="text-sm text-muted-foreground">{label}</p>
  </div>
);

export default LiveTab;
