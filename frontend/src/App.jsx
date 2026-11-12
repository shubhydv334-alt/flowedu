import React from "react";
import { Routes, Route, Navigate, Outlet, Link, useLocation } from "react-router-dom";
import { TopNav, Button } from "./ui";
import { useAuth } from "./auth";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Academics from "./pages/Academics";
import ImportPage from "./pages/ImportPage";
import Workflows from "./pages/Workflows";
import WorkflowBuilder from "./pages/WorkflowBuilder";
import Templates from "./pages/Templates";
import Logs from "./pages/Logs";
import StyleGuide from "./StyleGuide";

const MENU = {
  ADMIN: [
    ["Dashboard", "/dashboard"], ["Academics", "/academics"], ["Import", "/import"],
    ["Workflows", "/workflows"], ["Templates", "/templates"], ["Logs", "/logs"],
  ],
  TEACHER: [
    ["Dashboard", "/dashboard"], ["Academics", "/academics"], ["Import", "/import"],
    ["Workflows", "/workflows"], ["Templates", "/templates"], ["Logs", "/logs"],
  ],
  STUDENT: [["Dashboard", "/dashboard"], ["Academics", "/academics"]],
};

function Layout() {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();
  const items = (MENU[user.role] || MENU.STUDENT).map(([label, href]) => ({ label, href }));
  const active = items.find((i) => pathname.startsWith(i.href))?.href;
  return (
    <>
      <TopNav
        brand="FlowEdu"
        items={items}
        activePath={active}
        renderLink={(it, cls) => <Link key={it.href} to={it.href} className={cls}>{it.label}</Link>}
        actions={
          <>
            <span className="caption muted">{user.name} · {user.role.toLowerCase()}</span>
            <Button variant="outline" className="btn-sm" onClick={logout}>Sign out</Button>
          </>
        }
      />
      <main className="container page">
        <Outlet />
      </main>
    </>
  );
}

function Protected() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Layout />;
}

function Restricted({ children }) {
  const { canWrite } = useAuth();
  return canWrite ? children : <Navigate to="/dashboard" replace />;
}

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <Login />} />
      <Route path="/styleguide" element={<StyleGuide />} />
      <Route element={<Protected />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/academics" element={<Academics />} />
        <Route path="/import" element={<Restricted><ImportPage /></Restricted>} />
        <Route path="/workflows" element={<Restricted><Workflows /></Restricted>} />
        <Route path="/workflows/new" element={<Restricted><WorkflowBuilder /></Restricted>} />
        <Route path="/workflows/:id" element={<Restricted><WorkflowBuilder /></Restricted>} />
        <Route path="/templates" element={<Restricted><Templates /></Restricted>} />
        <Route path="/logs" element={<Restricted><Logs /></Restricted>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
