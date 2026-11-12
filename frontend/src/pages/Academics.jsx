import React, { useMemo, useState } from "react";
import { Button, Card, Input, Table } from "../ui";
import { api, useApi } from "../api";
import { useAuth } from "../auth";
import { fmt } from "../meta";

const get = (o, p) => p.split(".").reduce((a, k) => (a == null ? a : a[k]), o);

const deptField = { name: "departmentId", label: "Department", from: "/departments", optionLabel: (o) => o.name };
const courseField = { name: "courseId", label: "Course", from: "/courses", optionLabel: (o) => `${o.code} · ${o.name}` };

const TABS = {
  departments: {
    title: "Departments", path: "/departments",
    columns: [{ key: "name", header: "Name", strong: true }],
    fields: [{ name: "name", label: "Department name" }],
  },
  courses: {
    title: "Courses", path: "/courses", pillKey: "department.name",
    columns: [
      { key: "code", header: "Code", strong: true }, { key: "name", header: "Name" },
      { key: "department.name", header: "Department" }, { key: "semester", header: "Sem" },
      { key: "teacherName", header: "Teacher" },
    ],
    fields: [
      { name: "code", label: "Course code" }, { name: "name", label: "Course name" }, deptField,
      { name: "semester", label: "Semester", type: "number" },
      { name: "teacherName", label: "Teacher name" }, { name: "teacherEmail", label: "Teacher email" },
      { name: "teacherTelegramChatId", label: "Teacher Telegram chat id" },
    ],
  },
  students: {
    title: "Students", path: "/students", pillKey: "department.name",
    columns: [
      { key: "rollNo", header: "Roll no", strong: true }, { key: "name", header: "Name" },
      { key: "department.name", header: "Dept" }, { key: "semester", header: "Sem" },
      { key: "section", header: "Section" }, { key: "batch", header: "Batch" }, { key: "email", header: "Email" },
    ],
    fields: [
      { name: "rollNo", label: "Roll number" }, { name: "name", label: "Name" }, { name: "email", label: "Email" },
      { name: "telegramChatId", label: "Telegram chat id" }, deptField,
      { name: "semester", label: "Semester", type: "number" }, { name: "section", label: "Section" },
      { name: "batch", label: "Batch" },
    ],
  },
  assignments: {
    title: "Assignments", path: "/assignments", pillKey: "course.code", notSubmitted: true,
    columns: [
      { key: "title", header: "Title", strong: true }, { key: "course.name", header: "Course" },
      { key: "deadline", header: "Deadline", render: (r) => fmt(r.deadline) },
    ],
    fields: [courseField, { name: "title", label: "Title" }, { name: "deadline", label: "Deadline", type: "datetime-local" }],
  },
  exams: {
    title: "Exams", path: "/exams", pillKey: "course.code",
    columns: [
      { key: "name", header: "Exam", strong: true }, { key: "course.name", header: "Course" },
      { key: "examDate", header: "Date" },
    ],
    fields: [courseField, { name: "name", label: "Exam name" }, { name: "examDate", label: "Exam date", type: "date" }],
  },
  attendance: {
    title: "Attendance", path: "/attendance", readOnly: true, pillKey: "course.code",
    columns: [
      { key: "student.rollNo", header: "Roll no", strong: true }, { key: "student.name", header: "Student" },
      { key: "course.code", header: "Course" }, { key: "percent", header: "Attendance %" },
      { key: "recordDate", header: "Date" },
    ],
    fields: [],
  },
};

function OptionSelect({ field, value, onChange }) {
  const { data } = useApi(field.from);
  return (
    <div className="field">
      <label>{field.label}</label>
      <select className="input" value={value ?? ""} onChange={(e) => onChange(e.target.value)}>
        <option value="">Select…</option>
        {(data || []).map((o) => <option key={o.id} value={o.id}>{field.optionLabel(o)}</option>)}
      </select>
    </div>
  );
}

function toBody(form, fields) {
  const body = {};
  for (const f of fields) {
    const v = form[f.name];
    if (v === undefined || v === "") { body[f.name] = null; continue; }
    body[f.name] = f.type === "number" || f.from ? Number(v) : v;
  }
  return body;
}

function Panel({ cfg, canWrite }) {
  const { data, error, reload } = useApi(cfg.path);
  const [form, setForm] = useState({});
  const [open, setOpen] = useState(false);
  const [msg, setMsg] = useState("");
  const [q, setQ] = useState("");
  const [pill, setPill] = useState("All");
  const [pending, setPending] = useState(null);

  const rows = data || [];
  const pills = useMemo(
    () => (cfg.pillKey ? ["All", ...new Set(rows.map((r) => get(r, cfg.pillKey)).filter(Boolean))] : []),
    [rows, cfg]
  );
  const shown = rows.filter(
    (r) => (pill === "All" || get(r, cfg.pillKey) === pill) && JSON.stringify(r).toLowerCase().includes(q.toLowerCase())
  );

  async function create(e) {
    e.preventDefault();
    setMsg("");
    try {
      await api(cfg.path, { method: "POST", body: toBody(form, cfg.fields) });
      setForm({});
      setOpen(false);
      reload();
    } catch (err) { setMsg(err.message); }
  }

  async function remove(id) {
    if (!window.confirm("Delete this record?")) return;
    try { await api(`${cfg.path}/${id}`, { method: "DELETE" }); reload(); }
    catch (err) { setMsg(err.message); }
  }

  async function showPending(a) {
    try { setPending({ a, list: await api(`/assignments/${a.id}/not-submitted`) }); }
    catch (err) { setMsg(err.message); }
  }

  async function markSubmitted(a, s) {
    try {
      await api(`/assignments/${a.id}/submit`, { method: "POST", body: { studentId: s.id } });
      showPending(a);
    } catch (err) { setMsg(err.message); }
  }

  const columns = [
    ...cfg.columns.map((c) => ({ ...c, render: c.render || ((r) => get(r, c.key) ?? "") })),
    ...(canWrite && !cfg.readOnly
      ? [{
          key: "_actions", header: "",
          render: (r) => (
            <div className="row" style={{ justifyContent: "flex-end", gap: 8 }}>
              {cfg.notSubmitted && <Button variant="outline" className="btn-sm" onClick={() => showPending(r)}>Not submitted</Button>}
              <Button variant="text" onClick={() => remove(r.id)}>Delete</Button>
            </div>
          ),
        }]
      : []),
  ];

  return (
    <div className="stack">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <div className="row">
          <input className="input" style={{ width: 240, height: 36 }} placeholder="Search…" value={q} onChange={(e) => setQ(e.target.value)} />
          {pills.length > 1 && (
            <div className="pills">
              {pills.map((p) => (
                <button key={p} className={`pill${pill === p ? " active" : ""}`} onClick={() => setPill(p)}>{p}</button>
              ))}
            </div>
          )}
        </div>
        {canWrite && !cfg.readOnly && (
          <Button onClick={() => setOpen(!open)}>{open ? "Close" : `Add ${cfg.title.toLowerCase().replace(/s$/, "")}`}</Button>
        )}
      </div>

      {open && (
        <Card>
          <form onSubmit={create} className="stack">
            <div className="grid grid-3">
              {cfg.fields.map((f) =>
                f.from ? (
                  <OptionSelect key={f.name} field={f} value={form[f.name]} onChange={(v) => setForm({ ...form, [f.name]: v })} />
                ) : (
                  <Input key={f.name} label={f.label} type={f.type || "text"} value={form[f.name] ?? ""}
                         onChange={(e) => setForm({ ...form, [f.name]: e.target.value })} />
                )
              )}
            </div>
            <div className="row"><Button type="submit">Save</Button></div>
          </form>
        </Card>
      )}

      {(msg || error) && <span className="msg-err">{msg || error}</span>}

      <Card style={{ padding: 0 }}>
        {shown.length === 0 ? <div className="empty muted">Nothing here yet.</div> : <Table columns={columns} rows={shown} />}
      </Card>

      {pending && (
        <Card>
          <div className="card-head">
            <h3>Not submitted: {pending.a.title}</h3>
            <Button variant="text" onClick={() => setPending(null)}>Close</Button>
          </div>
          {pending.list.length === 0 ? (
            <p className="muted">Everyone has submitted.</p>
          ) : (
            <Table
              columns={[
                { key: "rollNo", header: "Roll no", strong: true }, { key: "name", header: "Name" }, { key: "email", header: "Email" },
                ...(canWrite ? [{ key: "_a", header: "", render: (s) => <Button variant="outline" className="btn-sm" onClick={() => markSubmitted(pending.a, s)}>Mark submitted</Button> }] : []),
              ]}
              rows={pending.list}
            />
          )}
          {canWrite && <p className="caption muted" style={{ marginTop: 12 }}>Marking a submission fires workflows that listen for &quot;Assignment submitted&quot;.</p>}
        </Card>
      )}
    </div>
  );
}

export default function Academics() {
  const { canWrite } = useAuth();
  const [tab, setTab] = useState("students");
  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="display-lg">Academics</h1>
          <p className="muted">Departments, courses, students, assignments, exams and attendance.</p>
        </div>
      </div>
      <div className="pills" style={{ marginBottom: 24 }}>
        {Object.entries(TABS).map(([k, v]) => (
          <button key={k} className={`pill${tab === k ? " active" : ""}`} onClick={() => setTab(k)}>{v.title}</button>
        ))}
      </div>
      <Panel key={tab} cfg={TABS[tab]} canWrite={canWrite} />
    </>
  );
}
