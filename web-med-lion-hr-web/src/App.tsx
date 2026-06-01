import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, useEffect } from "react";

import { Toaster } from "@/components/ui/sonner";
import Dashboard from "@/pages/Dashboard";
import Login from "@/pages/Login";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { refetchInterval: 5000, refetchOnWindowFocus: true, retry: 1 },
  },
});

const App = () => {
  const [authed, setAuthed] = useState<boolean>(false);

  useEffect(() => {
    if (sessionStorage.getItem("mlhr_admin") === "1") setAuthed(true);
  }, []);

  const handleLogin = () => {
    sessionStorage.setItem("mlhr_admin", "1");
    setAuthed(true);
  };

  const handleLogout = () => {
    sessionStorage.removeItem("mlhr_admin");
    setAuthed(false);
  };

  return (
    <QueryClientProvider client={queryClient}>
      {/* Premium floating ambient orbs — live background layers */}
      <div className="pointer-events-none fixed inset-0 z-0 overflow-hidden">
        <div className="absolute left-[10%] top-[8%] h-[600px] w-[600px] animate-float rounded-full bg-gradient-radial from-primary/8 to-transparent blur-3xl" />
        <div className="absolute right-[5%] top-[60%] h-[500px] w-[500px] animate-float rounded-full bg-gradient-radial from-success/6 to-transparent blur-3xl" style={{ animationDelay: "-4s" }} />
        <div className="absolute bottom-[5%] left-[40%] h-[400px] w-[400px] animate-float rounded-full bg-gradient-radial from-destructive/5 to-transparent blur-3xl" style={{ animationDelay: "-8s" }} />
      </div>

      <Toaster position="top-center" theme="dark" richColors />
      <div className="relative z-10">
        {authed ? <Dashboard onLogout={handleLogout} /> : <Login onLogin={handleLogin} />}
      </div>
    </QueryClientProvider>
  );
};

export default App;
