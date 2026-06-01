import { useState } from "react";
import { useQueryClient, useQuery, useMutation } from "@tanstack/react-query";
import { UserPlus, Trash2, X } from "lucide-react";
import { toast } from "sonner";

import { api } from "@/lib/api";

const EmployeesTab = () => {
  const qc = useQueryClient();
  const { data: employees = [] } = useQuery({ queryKey: ["employees"], queryFn: api.employees });
  const [showForm, setShowForm] = useState<boolean>(false);
  const [form, setForm] = useState({ employeeId: "", password: "", name: "", role: "", baseSalary: "" });

  const create = useMutation({
    mutationFn: () =>
      api.createEmployee({
        employeeId: form.employeeId.trim(),
        password: form.password,
        name: form.name.trim(),
        role: form.role.trim() || "Staff",
        baseSalary: parseFloat(form.baseSalary) || 0,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      toast.success("Employee created");
      setForm({ employeeId: "", password: "", name: "", role: "", baseSalary: "" });
      setShowForm(false);
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : "Failed to create"),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api.deleteEmployee(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      toast.success("Employee removed");
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : "Failed"),
  });

  const canSubmit = form.employeeId.trim() && form.password.length >= 4 && form.name.trim();

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Employees</h2>
          <p className="text-sm text-muted-foreground">{employees.length} member(s)</p>
        </div>
        <button
          onClick={() => setShowForm((s) => !s)}
          className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-all hover:brightness-110"
        >
          {showForm ? <X className="h-4 w-4" /> : <UserPlus className="h-4 w-4" />}
          {showForm ? "Cancel" : "Add"}
        </button>
      </div>

      {showForm && (
        <div className="glass animate-fade-up space-y-3 p-5">
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <Field label="Login ID" value={form.employeeId} onChange={(v) => setForm({ ...form, employeeId: v })} placeholder="EMP006" />
            <Field label="Password (min 4)" value={form.password} onChange={(v) => setForm({ ...form, password: v })} placeholder="••••••" />
            <Field label="Full name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} placeholder="Jane Doe" />
            <Field label="Role" value={form.role} onChange={(v) => setForm({ ...form, role: v })} placeholder="Engineer" />
            <Field label="Base salary (₹/month)" value={form.baseSalary} onChange={(v) => setForm({ ...form, baseSalary: v.replace(/[^0-9.]/g, "") })} placeholder="60000" />
          </div>
          <button
            onClick={() => create.mutate()}
            disabled={!canSubmit || create.isPending}
            className="w-full rounded-xl bg-primary py-3 text-sm font-semibold text-primary-foreground transition-all hover:brightness-110 disabled:opacity-50"
          >
            {create.isPending ? "Creating…" : "Create employee"}
          </button>
        </div>
      )}

      <div className="space-y-3">
        {employees.map((emp) => {
          const color = `#${(emp.avatarColor & 0xffffff).toString(16).padStart(6, "0")}`;
          return (
            <div key={emp.id} className="glass glass-hover flex items-center gap-4 p-4">
              <div
                className="flex h-11 w-11 items-center justify-center rounded-full text-sm font-bold"
                style={{ background: `${color}33`, color }}
              >
                {emp.name.charAt(0)}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{emp.name}</p>
                <p className="truncate text-xs text-muted-foreground">
                  {emp.employeeId} · {emp.role} · ₹{emp.baseSalary.toLocaleString("en-IN")}/mo
                </p>
              </div>
              <button
                onClick={() => {
                  if (confirm(`Remove ${emp.name}? This deletes all their data.`)) remove.mutate(emp.id);
                }}
                className="rounded-lg p-2.5 text-destructive transition-all hover:bg-destructive/15"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const Field = ({ label, value, onChange, placeholder }: { label: string; value: string; onChange: (v: string) => void; placeholder: string }) => (
  <div>
    <label className="mb-1.5 block text-xs font-medium text-muted-foreground">{label}</label>
    <input
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="glass w-full rounded-xl px-3.5 py-2.5 text-sm outline-none placeholder:text-muted-foreground/60 focus:border-primary/50"
    />
  </div>
);

export default EmployeesTab;
