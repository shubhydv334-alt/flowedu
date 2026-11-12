import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Badge, Button, Card, Table } from "../ui";
import { api, useApi } from "../api";
import { fmt, triggerSummary } from "../meta";

export default function Workflows() {
  const { data, error, reload } = useApi("/workflows");
  const nav = useNavigate();
  const [msg, setMsg] = useState("");

  async function act(label, fn) {
    setMsg("");
    try { await fn(); await reload(); if (label) setMsg(label); }
    catch (e) { setMsg(`Error: ${e.message}`); }
  }

  const columns = [
    {
      key: "name", header: "Workflow", strong: true,
      render: (w) => <Link to={`/workflows/${w.id}`} style={{ color: "inherit", fontWeight: 500 }}>{w.name}</Link>,
    },
    { key: "trigger", header: "Trigger", render: (w) => triggerSummary(w.trigger) },
    { key: "steps", header: "Actions", render: (w) => w.steps.map((s) => s.type).join(" → ") },
    { key: "lastRunAt", header: "Last run", render: (w) => fmt(w.lastRunAt) },
    { key: "enabled", header: "Status", render: (w) => <Badge tone={w.enabled ? "success" : "default"}>{w.enabled ? "Active" : "Disabled"}</Badge> },
    {
      key: "_a", header: "",
      render: (w) => (
        <div className="row" style={{ gap: 6, justifyContent: "flex-end" }}>
          <Button variant="outline" className="btn-sm" onClick={() => act(w.enabled ? "Disabled" : "Enabled", () => api(`/workflows/${w.id}/toggle`, { method: "POST" }))}>
            {w.enabled ? "Disable" : "Enable"}
          </Button>
          <Button variant="outline" className="btn-sm" onClick={() => act("Run started, see Logs", () => api(`/workflows/${w.id}/run`, { method: "POST" }))}>Run now</Button>
          <Button variant="outline" className="btn-sm" onClick={() => act("Cloned", () => api(`/workflows/${w.id}/clone`, { method: "POST" }))}>Clone</Button>
          <Button variant="outline" className="btn-sm" onClick={() => nav(`/workflows/${w.id}`)}>Edit</Button>
          <Button variant="text" onClick={() => window.confirm(`Delete "${w.name}"?`) && act("Deleted", () => api(`/workflows/${w.id}`, { method: "DELETE" }))}>Delete</Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="display-lg">Workflows</h1>
          <p className="muted">Trigger, conditions, audience and actions. Test before you enable.</p>
        </div>
        <div className="row">
          <Link className="btn btn-outline" to="/templates">From template</Link>
          <Link className="btn btn-primary" to="/workflows/new">New workflow</Link>
        </div>
      </div>
      {msg && <p className={msg.startsWith("Error") ? "msg-err" : "msg-ok"} style={{ marginBottom: 12 }}>{msg}</p>}
      {error && <p className="msg-err">{error}</p>}
      <Card style={{ padding: 0 }}>
        {!data || data.length === 0
          ? <div className="empty muted">No workflows yet. Start from a template.</div>
          : <Table columns={columns} rows={data} />}
      </Card>
    </>
  );
}
