import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Badge, Button, Card, Input } from "../ui";
import { api, useApi } from "../api";
import {
  CONDITION_FIELDS, CRON_PRESETS, DATE_SOURCES, EVENTS, OPERATORS, STEP_TYPES, VARIABLES, tone,
} from "../meta";

const blank = () => ({
  name: "", description: "",
  trigger: { type: "EVENT", event: "ATTENDANCE_UPLOADED", source: "ATTENDANCE" },
  conditions: { logic: "AND", rules: [] },
  audience: {},
  steps: [],
});

const newStep = (type) => ({
  type,
  config:
    type === "AI" ? { task: "personalize", tone: "friendly", prompt: "", fallback: "" }
    : type === "EMAIL" ? { recipient: "STUDENT", subject: "", body: "" }
    : type === "TELEGRAM" ? { recipient: "STUDENT", text: "" }
    : { url: "", method: "POST", body: "{}" },
});

function Select({ label, value, onChange, options }) {
  return (
    <div className="field">
      <label>{label}</label>
      <select className="input" value={value ?? ""} onChange={(e) => onChange(e.target.value)}>
        {options.map((o) => <option key={o.value ?? o} value={o.value ?? o}>{o.label ?? o}</option>)}
      </select>
    </div>
  );
}

function Section({ n, title, hint, children }) {
  return (
    <Card>
      <div className="card-head">
        <span className="num">{n}</span>
        <div>
          <h3>{title}</h3>
          {hint && <span className="caption muted">{hint}</span>}
        </div>
      </div>
      {children}
    </Card>
  );
}

export default function WorkflowBuilder() {
  const { id } = useParams();
  const nav = useNavigate();
  const [wf, setWf] = useState(blank());
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const focus = useRef(null);
  const depts = useApi("/departments");

  useEffect(() => {
    if (id) api(`/workflows/${id}`).then(setWf).catch((e) => setErr(e.message));
    else setWf(blank());
  }, [id]);

  const set = (patch) => setWf((w) => ({ ...w, ...patch }));
  const setTrigger = (patch) => setWf((w) => ({ ...w, trigger: { ...w.trigger, ...patch } }));
  const setAudience = (patch) => setWf((w) => ({ ...w, audience: { ...w.audience, ...patch } }));

  function changeTriggerType(type) {
    set({
      trigger:
        type === "EVENT" ? { type, event: "ATTENDANCE_UPLOADED", source: "ATTENDANCE" }
        : type === "SCHEDULE" ? { type, cron: "0 0 8 * * MON", source: "ATTENDANCE" }
        : { type, dateSource: "ASSIGNMENT_DEADLINE", source: "ASSIGNMENT", daysBefore: 2 },
    });
  }

  // conditions
  const rules = wf.conditions?.rules || [];
  const setRules = (r) => set({ conditions: { ...wf.conditions, rules: r } });
  const setRule = (i, patch) => setRules(rules.map((r, k) => (k === i ? { ...r, ...patch } : r)));
  const condText = rules.length
    ? rules.map((r) => `${r.field} ${r.op} ${r.value}`).join(` ${wf.conditions.logic || "AND"} `)
    : "Everyone in the audience";

  // steps
  const setStep = (i, patch) => set({ steps: wf.steps.map((s, k) => (k === i ? { ...s, ...patch } : s)) });
  const setCfg = (i, key, val) => setStep(i, { config: { ...wf.steps[i].config, [key]: val } });
  const move = (i, d) => {
    const s = [...wf.steps]; const j = i + d;
    if (j < 0 || j >= s.length) return;
    [s[i], s[j]] = [s[j], s[i]];
    set({ steps: s });
  };

  function insertVar(v) {
    const f = focus.current;
    if (!f) { setMsg("Click into a message field first, then pick a variable."); return; }
    const cur = wf.steps[f.i]?.config?.[f.key] || "";
    const pos = f.el.selectionStart ?? cur.length;
    setCfg(f.i, f.key, cur.slice(0, pos) + `{{${v}}}` + cur.slice(pos));
    setMsg("");
  }

  const field = (i, key, label, props = {}) => {
    const Tag = props.area ? "textarea" : "input";
    return (
      <div className="field" key={key}>
        <label>{label}</label>
        <Tag
          className="input"
          value={wf.steps[i].config[key] ?? ""}
          onFocus={(e) => { focus.current = { i, key, el: e.target }; }}
          onChange={(e) => setCfg(i, key, e.target.value)}
          placeholder={props.placeholder}
        />
      </div>
    );
  };

  async function save(enable) {
    setErr(""); setMsg(""); setBusy(true);
    try {
      const body = { ...wf, enabled: enable === undefined ? wf.enabled : enable };
      const saved = wf.id
        ? await api(`/workflows/${wf.id}`, { method: "PUT", body })
        : await api("/workflows", { method: "POST", body });
      setWf(saved);
      setMsg(enable === true ? "Saved and enabled" : "Saved");
      if (!wf.id) nav(`/workflows/${saved.id}`, { replace: true });
      return saved;
    } catch (e) { setErr(e.message); return null; }
    finally { setBusy(false); }
  }

  async function test(send) {
    const saved = await save();
    if (!saved) return;
    setBusy(true); setResult(null);
    try { setResult(await api(`/workflows/${saved.id}/test?send=${send}`, { method: "POST" })); }
    catch (e) { setErr(e.message); }
    finally { setBusy(false); }
  }

  const t = wf.trigger || {};
  const pretty = (s) => { try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s || ""; } };

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="display-lg">{wf.id ? "Edit workflow" : "New workflow"}</h1>
          <p className="muted">{wf.id ? (wf.enabled ? "Active" : "Disabled") : "Not saved yet"}</p>
        </div>
        <div className="row">
          <Button variant="outline" disabled={busy} onClick={() => nav("/workflows")}>Back</Button>
          <Button variant="outline" disabled={busy} onClick={() => save()}>Save</Button>
          <Button disabled={busy} onClick={() => save(!wf.enabled)}>{wf.enabled ? "Disable" : "Save and enable"}</Button>
        </div>
      </div>
      {err && <p className="msg-err">{err}</p>}
      {msg && <p className="msg-ok">{msg}</p>}

      <div className="stack">
        <Section n="1" title="Basics">
          <div className="grid grid-2">
            <Input label="Name" value={wf.name} onChange={(e) => set({ name: e.target.value })} placeholder="Low Attendance Alert" />
            <Input label="Description" value={wf.description || ""} onChange={(e) => set({ description: e.target.value })} placeholder="What does this do?" />
          </div>
        </Section>

        <Section n="2" title="Trigger" hint="What starts this workflow">
          <div className="grid grid-3">
            <Select label="Type" value={t.type} onChange={changeTriggerType}
              options={[{ value: "EVENT", label: "Event" }, { value: "SCHEDULE", label: "Schedule (cron)" }, { value: "DATE", label: "Date-based (X days before)" }]} />
            {t.type === "EVENT" && (
              <Select label="Event" value={t.event}
                onChange={(v) => setTrigger({ event: v, source: EVENTS.find((e) => e.key === v).source })}
                options={EVENTS.map((e) => ({ value: e.key, label: e.label }))} />
            )}
            {t.type === "SCHEDULE" && (
              <>
                <Input label="Cron (sec min hour day month weekday)" value={t.cron || ""} onChange={(e) => setTrigger({ cron: e.target.value })} />
                <Select label="Data" value={t.source} onChange={(v) => setTrigger({ source: v })} options={["ATTENDANCE", "ASSIGNMENT", "EXAM", "STUDENT"]} />
              </>
            )}
            {t.type === "DATE" && (
              <>
                <Select label="Before" value={t.dateSource}
                  onChange={(v) => setTrigger({ dateSource: v, source: DATE_SOURCES.find((d) => d.key === v).source })}
                  options={DATE_SOURCES.map((d) => ({ value: d.key, label: d.label }))} />
                <Input label="Days before" type="number" value={t.daysBefore ?? ""} onChange={(e) => setTrigger({ daysBefore: Number(e.target.value) })} />
              </>
            )}
          </div>
          {t.type === "SCHEDULE" && (
            <div className="pills" style={{ marginTop: 12 }}>
              {CRON_PRESETS.map((p) => <button key={p.cron} className="pill" onClick={() => setTrigger({ cron: p.cron })}>{p.label}</button>)}
            </div>
          )}
        </Section>

        <Section n="3" title="Conditions" hint="Who qualifies">
          <div className="row" style={{ marginBottom: 12 }}>
            <Select label="Match" value={wf.conditions?.logic || "AND"} onChange={(v) => set({ conditions: { ...wf.conditions, logic: v } })}
              options={[{ value: "AND", label: "All rules (AND)" }, { value: "OR", label: "Any rule (OR)" }]} />
          </div>
          <div className="stack" style={{ gap: 12 }}>
            {rules.map((r, i) => (
              <div className="grid grid-3" key={i} style={{ gridTemplateColumns: "2fr 1fr 1.5fr auto", alignItems: "end" }}>
                <Select label="Field" value={r.field} onChange={(v) => setRule(i, { field: v })}
                  options={CONDITION_FIELDS.map((f) => ({ value: f.key, label: f.label }))} />
                <Select label="Operator" value={r.op} onChange={(v) => setRule(i, { op: v })} options={OPERATORS} />
                <Input label="Value" value={r.value ?? ""} onChange={(e) => setRule(i, { value: e.target.value })} />
                <Button variant="text" onClick={() => setRules(rules.filter((_, k) => k !== i))}>Remove</Button>
              </div>
            ))}
          </div>
          <div className="row" style={{ marginTop: 12 }}>
            <Button variant="outline" className="btn-sm" onClick={() => setRules([...rules, { field: "attendance.percent", op: "<", value: "75" }])}>Add rule</Button>
            <span className="caption muted">Reads as: <code>{condText}</code></span>
          </div>
        </Section>

        <Section n="4" title="Audience" hint="Narrow it down by department, batch or section">
          <div className="grid grid-3">
            <Select label="Department" value={wf.audience?.departmentId ?? ""} onChange={(v) => setAudience({ departmentId: v || null })}
              options={[{ value: "", label: "All departments" }, ...((depts.data || []).map((d) => ({ value: d.id, label: d.name })))]} />
            <Input label="Batch" value={wf.audience?.batch ?? ""} onChange={(e) => setAudience({ batch: e.target.value })} placeholder="e.g. 2021" />
            <Input label="Section" value={wf.audience?.section ?? ""} onChange={(e) => setAudience({ section: e.target.value })} placeholder="e.g. A" />
          </div>
          <label className="row" style={{ marginTop: 12, gap: 8, cursor: "pointer" }}>
            <input type="checkbox" checked={!!wf.audience?.digest} onChange={(e) => setAudience({ digest: e.target.checked })} />
            <span className="caption">Digest: send one message per course (use <code>{"{{summary.list}}"}</code>, ideal for teachers)</span>
          </label>
        </Section>

        <Section n="5" title="Actions" hint="Run in order. Each step can use {{variables}} and the output of earlier steps">
          <div className="row" style={{ marginBottom: 16 }}>
            <span className="caption-uppercase muted">Variables</span>
            {VARIABLES.map((v) => (
              <button key={v} className="chip" onMouseDown={(e) => e.preventDefault()} onClick={() => insertVar(v)}>{`{{${v}}}`}</button>
            ))}
          </div>
          <div className="stack">
            {wf.steps.map((s, i) => (
              <Card key={i} style={{ background: "var(--color-canvas-soft)" }}>
                <div className="card-head">
                  <span className="num">{i + 1}</span>
                  <Badge>{s.type}</Badge>
                  <div style={{ flex: 1 }} />
                  <Button variant="text" onClick={() => move(i, -1)}>Up</Button>
                  <Button variant="text" onClick={() => move(i, 1)}>Down</Button>
                  <Button variant="text" onClick={() => set({ steps: wf.steps.filter((_, k) => k !== i) })}>Remove</Button>
                </div>
                <div className="stack" style={{ gap: 12 }}>
                  {s.type === "AI" && (
                    <>
                      <div className="grid grid-2">
                        <Select label="Task" value={s.config.task} onChange={(v) => setCfg(i, "task", v)}
                          options={[{ value: "personalize", label: "Personalized reminder" }, { value: "summarize", label: "Summary" }, { value: "rewrite", label: "Tone rewrite" }]} />
                        {field(i, "tone", "Tone", { placeholder: "friendly but firm" })}
                      </div>
                      {field(i, "prompt", "Facts / base text for the AI", { area: true })}
                      {field(i, "fallback", "Fallback text if AI is unavailable (plain template)", { area: true })}
                      <span className="caption muted">The result is available to later steps as <code>{"{{ai.message}}"}</code>.</span>
                    </>
                  )}
                  {s.type === "EMAIL" && (
                    <>
                      <div className="grid grid-2">
                        <Select label="Send to" value={s.config.recipient} onChange={(v) => setCfg(i, "recipient", v)}
                          options={[{ value: "STUDENT", label: "The student" }, { value: "TEACHER", label: "The course teacher" }, { value: "CUSTOM", label: "Custom address" }]} />
                        {s.config.recipient === "CUSTOM" && field(i, "to", "Email address")}
                      </div>
                      {field(i, "subject", "Subject")}
                      {field(i, "body", "Body", { area: true })}
                    </>
                  )}
                  {s.type === "TELEGRAM" && (
                    <>
                      <div className="grid grid-2">
                        <Select label="Send to" value={s.config.recipient} onChange={(v) => setCfg(i, "recipient", v)}
                          options={[{ value: "STUDENT", label: "The student" }, { value: "TEACHER", label: "The course teacher" }, { value: "CUSTOM", label: "Custom chat id" }]} />
                        {s.config.recipient === "CUSTOM" && field(i, "chatId", "Chat id")}
                      </div>
                      {field(i, "text", "Message", { area: true })}
                    </>
                  )}
                  {s.type === "WEBHOOK" && (
                    <>
                      <div className="grid grid-2">
                        {field(i, "url", "URL", { placeholder: "https://example.com/hook" })}
                        <Select label="Method" value={s.config.method} onChange={(v) => setCfg(i, "method", v)} options={["POST", "PUT", "GET"]} />
                      </div>
                      {field(i, "body", "JSON body", { area: true })}
                    </>
                  )}
                </div>
              </Card>
            ))}
          </div>
          <div className="row" style={{ marginTop: 16 }}>
            {STEP_TYPES.map((s) => (
              <Button key={s.key} variant="outline" className="btn-sm" onClick={() => set({ steps: [...wf.steps, newStep(s.key)] })}>+ {s.label}</Button>
            ))}
          </div>
        </Section>

        <Section n="6" title="Test run" hint="Check it before enabling. Saves first.">
          <div className="row">
            <Button variant="outline" disabled={busy} onClick={() => test(false)}>Preview (sends nothing)</Button>
            <Button variant="outline" disabled={busy} onClick={() => test(true)}>Send test to me</Button>
            <span className="caption muted">Uses up to 3 matching students. &quot;Send test to me&quot; emails only you.</span>
          </div>
          {result && (
            <div className="stack" style={{ marginTop: 16 }}>
              <div className="row">
                <Badge tone={tone(result.run.status)}>{result.run.status}</Badge>
                <span className="caption">{result.run.affected} student(s) matched, {result.run.failures} failed</span>
                {result.run.error && <span className="msg-err">{result.run.error}</span>}
              </div>
              {result.steps.length === 0 && <p className="muted">No students matched your conditions and audience. Check the rules, or upload attendance first.</p>}
              {result.steps.map((s) => (
                <Card key={s.id} style={{ padding: 16 }}>
                  <div className="row" style={{ marginBottom: 8 }}>
                    <span className="num">{s.stepIndex}</span><strong style={{ color: "var(--color-ink)" }}>{s.stepType}</strong>
                    <span className="muted">{s.studentName}</span><Badge tone={tone(s.status)}>{s.status}</Badge>
                  </div>
                  {s.error && <p className="msg-err" style={{ margin: "0 0 8px" }}>{s.error}</p>}
                  <pre className="json">{pretty(s.outputJson)}</pre>
                </Card>
              ))}
            </div>
          )}
        </Section>
      </div>
    </>
  );
}
