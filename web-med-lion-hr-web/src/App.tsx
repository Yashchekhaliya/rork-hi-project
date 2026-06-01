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
      <Toaster position="top-center" theme="dark" />
      {authed ? <Dashboard onLogout={handleLogout} /> : <Login onLogin={handleLogin} />}
    </QueryClientProvider>
  );
};

export default App;
