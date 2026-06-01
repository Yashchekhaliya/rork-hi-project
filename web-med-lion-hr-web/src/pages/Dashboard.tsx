import { useState } from "react";
import {
  LayoutDashboard, CalendarCheck, Wallet, Users, Settings,
  LogOut, ShieldCheck,
} from "lucide-react";

import LiveTab from "@/components/tabs/LiveTab";
import ApprovalsTab from "@/components/tabs/ApprovalsTab";
import PayrollTab from "@/components/tabs/PayrollTab";
import EmployeesTab from "@/components/tabs/EmployeesTab";
import SettingsTab from "@/components/tabs/SettingsTab";
import { cn } from "@/lib/utils";

type TabId = "live" | "approvals" | "payroll" | "employees" | "settings";

const TABS: { id: TabId; label: string; icon: typeof LayoutDashboard }[] = [
  { id: "live", label: "Live", icon: LayoutDashboard },
  { id: "approvals", label: "Approvals", icon: CalendarCheck },
  { id: "payroll", label: "Payroll", icon: Wallet },
  { id: "employees", label: "Employees", icon: Users },
  { id: "settings", label: "Settings", icon: Settings },
];

const Dashboard = ({ onLogout }: { onLogout: () => void }) => {
  const [tab, setTab] = useState<TabId>("live");

  return (
    <div className="relative z-10 mx-auto flex min-h-screen w-full max-w-7xl flex-col px-4 py-6 md:px-8">
      {/* Header */}
      <header className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="glass flex h-12 w-12 items-center justify-center rounded-xl border-primary/30 shadow-lg shadow-primary/10">
            <ShieldCheck className="h-6 w-6 text-primary glow-text" />
          </div>
          <div>
            <h1 className="text-lg font-bold leading-tight tracking-tight">Med Lion HR</h1>
            <div className="flex items-center gap-2">
              <span className="flex h-1.5 w-1.5 rounded-full bg-success animate-pulse-dot" />
              <p className="text-xs text-muted-foreground">Admin Control Center</p>
            </div>
          </div>
        </div>
        <button
          onClick={onLogout}
          className="glass-danger glass-hover flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium text-destructive transition-all hover:border-destructive/50"
        >
          <LogOut className="h-4 w-4" />
          <span className="hidden sm:inline">Log out</span>
        </button>
      </header>

      {/* Tab bar */}
      <nav className="glass mb-6 flex gap-1 overflow-x-auto p-1.5">
        {TABS.map((t) => {
          const Icon = t.icon;
          const active = tab === t.id;
          return (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={cn(
                "flex flex-1 items-center justify-center gap-2 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-medium transition-all duration-300",
                active
                  ? "btn-premium text-primary-foreground"
                  : "text-muted-foreground hover:bg-white/5 hover:text-foreground",
              )}
            >
              <Icon className="h-4 w-4" />
              <span className="hidden sm:inline">{t.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Content */}
      <main className="flex-1 animate-fade-up" key={tab}>
        {tab === "live" && <LiveTab />}
        {tab === "approvals" && <ApprovalsTab />}
        {tab === "payroll" && <PayrollTab />}
        {tab === "employees" && <EmployeesTab />}
        {tab === "settings" && <SettingsTab onLogout={onLogout} />}
      </main>

      <footer className="mt-8 text-center text-xs text-muted-foreground">
        Med Lion HR · running on your local network
      </footer>
    </div>
  );
};

export default Dashboard;
