import { useState } from "react";
import { useQueryClient, useQuery, useMutation } from "@tanstack/react-query";
import { Lock, MapPin, LogOut, Save } from "lucide-react";
import { toast } from "sonner";

import { api } from "@/lib/api";

const SettingsTab = ({ onLogout }: { onLogout: () => void }) => {
  const qc = useQueryClient();
  const { data: worksite } = useQuery({ queryKey: ["worksite"], queryFn: api.worksite });

  const [newPassword, setNewPassword] = useState<string>("");
  const [site, setSite] = useState({ name: "", lat: "", lon: "", radius: "" });
  const [siteInit, setSiteInit] = useState<boolean>(false);

  if (worksite && !siteInit) {
    setSite({
      name: worksite.name,
      lat: String(worksite.center.latitude),
      lon: String(worksite.center.longitude),
      radius: String(Math.round(worksite.radiusMeters)),
    });
    setSiteInit(true);
  }

  const changePassword = useMutation({
    mutationFn: () => api.changeAdminPassword(newPassword),
    onSuccess: () => {
      toast.success("Admin password updated");
      setNewPassword("");
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : "Failed"),
  });

  const saveSite = useMutation({
    mutationFn: () =>
      api.updateWorksite({
        name: site.name,
        lat: parseFloat(site.lat),
        lon: parseFloat(site.lon),
        radius: Math.max(50, parseFloat(site.radius) || 1000),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["worksite"] });
      toast.success("Worksite updated");
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : "Failed"),
  });

  return (
    <div className="max-w-2xl space-y-6">
      {/* Password */}
      <section className="glass p-5">
        <div className="mb-4 flex items-center gap-3">
          <div className="glass flex h-10 w-10 items-center justify-center rounded-xl border-primary/30">
            <Lock className="h-5 w-5 text-primary" />
          </div>
          <div>
            <p className="font-semibold">Admin password</p>
            <p className="text-xs text-muted-foreground">Shared with the Android admin panel</p>
          </div>
        </div>
        <div className="flex gap-2">
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="New password (min 4 chars)"
            className="glass flex-1 rounded-xl px-3.5 py-2.5 text-sm outline-none placeholder:text-muted-foreground/60 focus:border-primary/50"
          />
          <button
            onClick={() => changePassword.mutate()}
            disabled={newPassword.length < 4 || changePassword.isPending}
            className="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-all hover:brightness-110 disabled:opacity-50"
          >
            Save
          </button>
        </div>
      </section>

      {/* Worksite */}
      <section className="glass p-5">
        <div className="mb-4 flex items-center gap-3">
          <div className="glass flex h-10 w-10 items-center justify-center rounded-xl border-primary/30">
            <MapPin className="h-5 w-5 text-primary" />
          </div>
          <div>
            <p className="font-semibold">Worksite geofence</p>
            <p className="text-xs text-muted-foreground">Check-ins are validated against this</p>
          </div>
        </div>
        <div className="space-y-3">
          <SettingField label="Site name" value={site.name} onChange={(v) => setSite({ ...site, name: v })} />
          <div className="grid grid-cols-2 gap-3">
            <SettingField label="Latitude" value={site.lat} onChange={(v) => setSite({ ...site, lat: v })} />
            <SettingField label="Longitude" value={site.lon} onChange={(v) => setSite({ ...site, lon: v })} />
          </div>
          <SettingField label="Radius (meters)" value={site.radius} onChange={(v) => setSite({ ...site, radius: v })} />
          <button
            onClick={() => saveSite.mutate()}
            disabled={saveSite.isPending}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-3 text-sm font-semibold text-primary-foreground transition-all hover:brightness-110 disabled:opacity-50"
          >
            <Save className="h-4 w-4" /> Save worksite
          </button>
        </div>
      </section>

      <button
        onClick={onLogout}
        className="glass glass-hover flex w-full items-center justify-center gap-2 rounded-xl border-destructive/30 py-3.5 text-sm font-semibold text-destructive"
      >
        <LogOut className="h-4 w-4" /> Log out of admin
      </button>
    </div>
  );
};

const SettingField = ({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) => (
  <div>
    <label className="mb-1.5 block text-xs font-medium text-muted-foreground">{label}</label>
    <input
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="glass w-full rounded-xl px-3.5 py-2.5 text-sm outline-none focus:border-primary/50"
    />
  </div>
);

export default SettingsTab;
