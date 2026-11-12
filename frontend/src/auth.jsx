import React, { createContext, useContext, useState } from "react";
import { api, getAuth, setAuth } from "./api";

const Ctx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getAuth());

  const login = async (email, password) => {
    const d = await api("/auth/login", { method: "POST", body: { email, password } });
    setAuth(d);
    setUser(d);
  };
  const logout = () => {
    setAuth(null);
    setUser(null);
  };
  const canWrite = !!user && user.role !== "STUDENT";

  return <Ctx.Provider value={{ user, login, logout, canWrite }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);
