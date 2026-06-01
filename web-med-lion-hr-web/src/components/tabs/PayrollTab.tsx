import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, Download, FileSpreadsheet } from "lucide-react";

import { api, formatMoney, MONTHS, SHORT_MONTHS } from "@/lib/api";

const now = new Date();

const PayrollTab = () => {
  const [month, setMonth] = useState<number>(now.getMonth());
  const [year, setYear] = useState<number>(now.getFullYear());
  const [view, setView] = useState<"monthly" | "yearly">("monthly");

  const { data: salaries = [] } = useQuery({
    queryKey: ["salaries", month, year],
    queryFn: () => api.salaries(month, year),
  });
  const { data: summaries = [] } = useQuery({
    queryKey: ["summaries", year],
    queryFn: () => api.summaries(year),
    enabled: view === "yearly",
  });

  const prevMonth = () => {
    if (month === 0) { setMonth(11); setYear((y) => y - 1); }
    else setMonth((m) => m - 1);
  };
  const nextMonth = () => {
    if (month === 11) { setMonth(0); setYear((y) => y + 1); }
    else setMonth((m) => m + 1);
  };

  const totalNet = salaries.reduce((s, r) => s + r.netPayable, 0);

  return (
    <div className="space-y-6">
      {/* View toggle */}
      <div className="glass flex gap-1 p-1.5">
        {(["monthly", "yearly"] as const).map((v) => (
          <button
            key={v}
            onClick={() => setView(v)}
            className={`flex-1 rounded-xl py-2.5 text-sm font-medium capitalize transition-all ${
              view === v ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {v}
          </button>
        ))}
      </div>

      {view === "monthly" ? (
        <>
          {/* Month nav + export */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="glass flex items-center gap-1 p-1.5">
              <button onClick={prevMonth} className="rounded-lg p-2 hover:bg-white/5"><ChevronLeft className="h-4 w-4" /></button>
              <span className="min-w-[140px] text-center text-sm font-semibold">{MONTHS[month]} {year}</span>
              <button onClick={nextMonth} className="rounded-lg p-2 hover:bg-white/5"><ChevronRight className="h-4 w-4" /></button>
            </div>
            <div className="flex gap-2">
              <a href={`/api/export/payroll/${month}/${year}`} className="glass glass-hover flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium text-primary">
                <Download className="h-4 w-4" /> Payroll CSV
              </a>
              <a href={`/api/export/attendance/${month}/${year}`} className="glass glass-hover flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium">
                <FileSpreadsheet className="h-4 w-4" /> Attendance
              </a>
            </div>
          </div>

          <div className="glass p-5">
            <p className="text-sm text-muted-foreground">Total payable · {MONTHS[month]}</p>
            <p className="text-3xl font-bold tracking-tight glow-text">{formatMoney(totalNet)}</p>
          </div>

          <div className="space-y-3">
            {salaries.map((r) => (
              <div key={r.employeeId} className="glass p-5">
                <div className="mb-3 flex items-center justify-between">
                  <p className="font-semibold">{r.employeeName}</p>
                  <p className="text-lg font-bold text-primary">{formatMoney(r.netPayable)}</p>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
                  <Metric label="Worked" value={`${r.daysWorked.toFixed(1)} / ${r.totalWorkingDays}`} />
                  <Metric label="Paid leave" value={`${r.paidLeaveDays}d`} />
                  <Metric label="Unpaid / absent" value={`${r.unpaidLeaveDays + Math.round(r.absentDays)}d`} />
                  <Metric label="Deductions" value={formatMoney(r.deductions)} negative={r.deductions > 0} />
                </div>
              </div>
            ))}
          </div>
        </>
      ) : (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="glass flex items-center gap-1 p-1.5">
              <button onClick={() => setYear((y) => y - 1)} className="rounded-lg p-2 hover:bg-white/5"><ChevronLeft className="h-4 w-4" /></button>
              <span className="min-w-[80px] text-center text-sm font-semibold">{year}</span>
              <button onClick={() => setYear((y) => y + 1)} className="rounded-lg p-2 hover:bg-white/5"><ChevronRight className="h-4 w-4" /></button>
            </div>
            <a href={`/api/export/summary/${year}`} className="glass glass-hover flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium text-primary">
              <Download className="h-4 w-4" /> Yearly CSV
            </a>
          </div>

          <div className="space-y-3">
            {summaries.map((s) => (
              <div key={s.employeeId} className="glass p-5">
                <div className="mb-3 flex items-center justify-between">
                  <p className="font-semibold">{s.employeeName}</p>
                  <p className="text-lg font-bold text-primary">{formatMoney(s.totalNet)}</p>
                </div>
                <div className="flex items-end gap-1.5">
                  {s.monthlyNet.map((v, i) => {
                    const max = Math.max(...s.monthlyNet, 1);
                    return (
                      <div key={i} className="flex flex-1 flex-col items-center gap-1">
                        <div
                          className="w-full rounded-t bg-primary/60"
                          style={{ height: `${Math.max(4, (v / max) * 60)}px` }}
                          title={formatMoney(v)}
                        />
                        <span className="text-[9px] text-muted-foreground">{SHORT_MONTHS[i].charAt(0)}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

const Metric = ({ label, value, negative }: { label: string; value: string; negative?: boolean }) => (
  <div className="rounded-xl bg-white/5 p-3">
    <p className="text-xs text-muted-foreground">{label}</p>
    <p className={`mt-0.5 font-semibold ${negative ? "text-destructive" : ""}`}>{value}</p>
  </div>
);

export default PayrollTab;
