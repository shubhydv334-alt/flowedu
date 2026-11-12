import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Badge, Button, Card, GradientOrb } from "../ui";
import { api, useApi } from "../api";
import { triggerSummary } from "../meta";

const ORBS = ["mint", "peach", "lavender", "sky"];

export default function Templates() {
  const { data, error } = useApi("/templates");
  const nav = useNavigate();
  const [busy, setBusy] = useState("");
  const [err, setErr] = useState("");

  async function use(key) {
    setBusy(key); setErr("");
    try {
      const wf = await api(`/templates/${key}/use`, { method: "POST" });
      nav(`/workflows/${wf.id}`);
    } catch (e) { setErr(e.message); setBusy(""); }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="display-lg">Templates</h1>
          <p className="muted">One click creates a ready workflow (disabled). Test it, then enable it.</p>
        </div>
      </div>
      {(err || error) && <p className="msg-err">{err || error}</p>}
      <div className="grid grid-2">
        {(data || []).map((t, i) => (
          <Card key={t.key} className="template-card orb-host" hover>
            <GradientOrb color={ORBS[i % ORBS.length]} size={220} top={-90} right={-60} />
            <div className="orb-content stack" style={{ gap: 12 }}>
              <div className="row"><Badge>{t.badge}</Badge></div>
              <h3 className="display-sm">{t.name}</h3>
              <p style={{ margin: 0 }}>{t.description}</p>
              <p className="caption muted" style={{ margin: 0 }}>
                {triggerSummary(t.trigger)} · {t.steps.map((s) => s.type).join(" → ")}
              </p>
              <div><Button disabled={!!busy} onClick={() => use(t.key)}>{busy === t.key ? "Creating…" : "Use template"}</Button></div>
            </div>
          </Card>
        ))}
      </div>
    </>
  );
}
