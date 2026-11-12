import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, Table } from "../ui";
import { api } from "../api";

const TYPES = {
  students: { label: "Students", headers: "rollNo,name,email,telegramChatId,department,semester,section,batch" },
  attendance: { label: "Attendance", headers: "rollNo,courseCode,percent,date   (date optional, yyyy-mm-dd)" },
  marks: { label: "Marks", headers: "rollNo,courseCode,exam,marks,maxMarks   (maxMarks optional)" },
};

export default function ImportPage() {
  const [type, setType] = useState("attendance");
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  async function send(isPreview) {
    if (!file) { setErr("Choose a CSV file first"); return; }
    setErr(""); setBusy(true);
    const form = new FormData();
    form.append("file", file);
    try {
      const d = await api(`/import/${type}?preview=${isPreview}`, { method: "POST", form });
      if (isPreview) { setPreview(d); setResult(null); } else { setResult(d); setPreview(null); }
    } catch (e) { setErr(e.message); }
    finally { setBusy(false); }
  }

  const shown = preview || result;
  const cols = shown && shown.preview.length ? Object.keys(shown.preview[0]).map((k) => ({ key: k, header: k })) : [];

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="display-lg">Import CSV</h1>
          <p className="muted">Preview first, then import. Uploading attendance fires every enabled "Attendance uploaded" workflow.</p>
        </div>
      </div>

      <div className="stack">
        <Card>
          <div className="pills" style={{ marginBottom: 16 }}>
            {Object.entries(TYPES).map(([k, v]) => (
              <button key={k} className={`pill${type === k ? " active" : ""}`}
                      onClick={() => { setType(k); setPreview(null); setResult(null); }}>{v.label}</button>
            ))}
          </div>
          <p className="caption muted">Columns: <code>{TYPES[type].headers}</code></p>
          <div className="row" style={{ marginTop: 16 }}>
            <input type="file" accept=".csv,text/csv" onChange={(e) => { setFile(e.target.files[0]); setPreview(null); setResult(null); }} />
            <Button variant="outline" disabled={busy} onClick={() => send(true)}>Preview</Button>
            <Button disabled={busy || !preview || preview.valid === 0} onClick={() => send(false)}>
              {preview ? `Import ${preview.valid} rows` : "Import"}
            </Button>
          </div>
          {err && <p className="msg-err" style={{ marginTop: 12 }}>{err}</p>}
        </Card>

        {shown && (
          <Card>
            <div className="card-head">
              <h3>{result ? "Import finished" : "Preview"}</h3>
              <Badge>{shown.total} rows</Badge>
              <Badge tone="success">{shown.valid} valid</Badge>
              {shown.invalid > 0 && <Badge tone="error">{shown.invalid} with errors</Badge>}
            </div>
            {result && (
              <p className="msg-ok" style={{ marginBottom: 12 }}>
                Imported {result.imported} rows.{" "}
                {type === "attendance" && <>Workflows were triggered, see <Link to="/logs" style={{ color: "inherit" }}>Logs</Link>.</>}
              </p>
            )}
            {shown.errors.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <span className="caption-uppercase muted">Errors</span>
                <ul style={{ margin: "8px 0 0", paddingLeft: 20 }}>
                  {shown.errors.map((e, i) => <li key={i} className="msg-err">Line {e.line}: {e.message}</li>)}
                </ul>
              </div>
            )}
            {cols.length > 0 && <div style={{ border: "1px solid var(--color-hairline)", borderRadius: 12, overflow: "hidden" }}><Table columns={cols} rows={shown.preview} rowKey="__none" /></div>}
            <p className="caption muted" style={{ marginTop: 8 }}>Showing the first {shown.preview.length} rows.</p>
          </Card>
        )}
      </div>
    </>
  );
}
