import { useState } from "react";
import { Lock, ShieldCheck } from "lucide-react";
import { toast } from "sonner";

import { api } from "@/lib/api";

/*** Default admin password — also used as client-side fallback when the server is unreachable. */
const DEFAULT_ADMIN_PASSWORD = "Yashwant@2000";

const Login = ({ onLogin }: { onLogin: () => void }) => {
  const [password, setPassword] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password) return;
    setLoading(true);
    try {
      await api.adminLogin(password);
      toast.success("Welcome back, admin");
      onLogin();
    } catch (err) {
      // If the server is unreachable (e.g. preview/demo mode), fall back to
      // checking against the default password client-side.
      const message = err instanceof Error ? err.message : "Login failed";
      const isNetworkError =
        message.includes("fetch failed") ||
        message.includes("NetworkError") ||
        message.includes("Failed to fetch") ||
        message.includes("404") ||
        message.includes("500");

      if (isNetworkError && password === DEFAULT_ADMIN_PASSWORD) {
        toast.success("Welcome back, admin (offline mode)");
        onLogin();
      } else if (isNetworkError) {
        toast.error("Server unreachable — check that the Med Lion server is running, or use the default password for offline access.");
      } else {
        toast.error(message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="glass animate-fade-up w-full max-w-md p-8">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="glass mb-5 flex h-16 w-16 items-center justify-center rounded-2xl border-primary/30">
            <ShieldCheck className="h-8 w-8 text-primary glow-text" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">Med Lion HR</h1>
          <p className="mt-1 text-sm text-muted-foreground">Admin Control Center</p>
        </div>

        <form onSubmit={submit} className="space-y-4">
          <div className="relative">
            <Lock className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Admin password"
              autoFocus
              className="glass w-full rounded-xl py-3.5 pl-11 pr-4 text-sm outline-none placeholder:text-muted-foreground focus:border-primary/50"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-primary py-3.5 text-sm font-semibold text-primary-foreground transition-all hover:brightness-110 disabled:opacity-60"
          >
            {loading ? "Verifying…" : "Enter Dashboard"}
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          Local network access · same password as the Android admin panel
        </p>
      </div>
    </div>
  );
};

export default Login;
