import { useQueryClient, useQuery, useMutation } from "@tanstack/react-query";
import { Check, X, CalendarDays } from "lucide-react";
import { toast } from "sonner";

import { api, type LeaveRequest, type LeaveStatus, type LeavePayType } from "@/lib/api";
import { cn } from "@/lib/utils";

function leaveDays(l: LeaveRequest): number {
  return Math.max(1, Math.floor((l.endMillis - l.startMillis) / 86_400_000) + 1);
}

function fmtDate(ms: number): string {
  return new Date(ms).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

const ApprovalsTab = () => {
  const qc = useQueryClient();
  const { data: leaves = [] } = useQuery({ queryKey: ["leaves"], queryFn: api.leaves });

  const decide = useMutation({
    mutationFn: ({ id, status, payType }: { id: string; status: LeaveStatus; payType: LeavePayType | null }) =>
      api.decideLeave(id, status, payType),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaves"] });
      qc.invalidateQueries({ queryKey: ["employees"] });
      toast.success("Leave updated");
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : "Failed"),
  });

  const pending = leaves.filter((l) => l.status === "PENDING");
  const decided = leaves.filter((l) => l.status !== "PENDING");

  return (
    <div className="space-y-6">
      <section>
        <h2 className="mb-3 text-lg font-semibold">
          Pending requests {pending.length > 0 && <span className="text-primary">({pending.length})</span>}
        </h2>
        {pending.length === 0 ? (
          <div className="glass p-8 text-center text-sm text-muted-foreground">No pending leave requests.</div>
        ) : (
          <div className="space-y-3">
            {pending.map((l) => (
              <div key={l.id} className="glass p-5">
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{l.employeeName}</p>
                    <p className="text-sm text-muted-foreground">{l.type} · {leaveDays(l)} day(s)</p>
                  </div>
                  <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <CalendarDays className="h-4 w-4" />
                    {fmtDate(l.startMillis)} – {fmtDate(l.endMillis)}
                  </div>
                </div>
                <p className="mb-4 rounded-xl bg-white/5 p-3 text-sm text-muted-foreground">{l.reason}</p>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => decide.mutate({ id: l.id, status: "APPROVED", payType: "WITH_PAY" })}
                    className="flex items-center gap-1.5 rounded-xl bg-emerald-500/20 px-4 py-2 text-sm font-medium text-emerald-400 transition-all hover:bg-emerald-500/30"
                  >
                    <Check className="h-4 w-4" /> Approve · With pay
                  </button>
                  <button
                    onClick={() => decide.mutate({ id: l.id, status: "APPROVED", payType: "WITHOUT_PAY" })}
                    className="flex items-center gap-1.5 rounded-xl bg-amber-500/20 px-4 py-2 text-sm font-medium text-amber-400 transition-all hover:bg-amber-500/30"
                  >
                    <Check className="h-4 w-4" /> Approve · Unpaid
                  </button>
                  <button
                    onClick={() => decide.mutate({ id: l.id, status: "REJECTED", payType: null })}
                    className="flex items-center gap-1.5 rounded-xl bg-destructive/20 px-4 py-2 text-sm font-medium text-destructive transition-all hover:bg-destructive/30"
                  >
                    <X className="h-4 w-4" /> Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-semibold">History</h2>
        <div className="space-y-2">
          {decided.map((l) => (
            <div key={l.id} className="glass flex items-center justify-between p-4">
              <div>
                <p className="font-medium">{l.employeeName}</p>
                <p className="text-xs text-muted-foreground">{l.type} · {fmtDate(l.startMillis)} – {fmtDate(l.endMillis)}</p>
              </div>
              <span
                className={cn(
                  "rounded-lg px-3 py-1 text-xs font-semibold",
                  l.status === "APPROVED" ? "bg-emerald-500/20 text-emerald-400" : "bg-destructive/20 text-destructive",
                )}
              >
                {l.status === "APPROVED"
                  ? l.payType === "WITH_PAY" ? "Paid" : "Unpaid"
                  : "Rejected"}
              </span>
            </div>
          ))}
          {decided.length === 0 && (
            <div className="glass p-6 text-center text-sm text-muted-foreground">No history yet.</div>
          )}
        </div>
      </section>
    </div>
  );
};

export default ApprovalsTab;
