import React, { useEffect, useState } from "react";
import { Badge, Button, Card } from "../ui";
import { useApi } from "../api";
import { fmt, tone } from "../meta";

const pretty = (s) => {
  try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s || ""; }
};

function RunDetails({ id }) {
  const { data, error } = useApi(`/runs/${id}`);
  if (error) return <p className="msg-err">{error}</p>;
  if (!data) return <p className="muted">Loading…</p>;
  if (data.steps.length === 0) {
    return <p className="muted">No steps ran. {data.run.error ? `Error: ${data.run.error}` : "No students matched the conditions."}</p>;
  }
  return (
    <div className="stack">
      {data.run.error && <p className="msg-err">Run error: {data.run.error}</p>}
      {data.steps.map((s) => (
        <Card key={s.id} style={{ padding: 16 }}>
          <div className="row" style={{ marginBottom: 12 }}>
            <span className="num">{s.stepIndex}</span>
            <strong style={{ color: "var(--color-ink)" }}>{s.stepType}</strong>
            <span className="muted">{s.studentName}</span>
            <Badge tone={tone(s.status)}>{s.status}</Badge>
            <span className="caption muted">{s.attempts} attempt{s.attempts === 1 ? "" : "s"} · {fmt(s.loggedAt)}</span>
          </div>
          {s.error && <p className="msg-err" style={{ margin: "0 0 12px" }}>{s.error}</p>}
          <div className="grid grid-2">
            <div><span className="caption-uppercase muted">Input</span><pre className="json">{pretty(s.inputJson)}</pre></div>
            <div><span className="caption-uppercase muted">Output</span><pre className="json">{pretty(s.outputJson)}</pre></div>
          </div>
        </Card>
      ))}
    </div>
  );
}

export default function Logs() {
  const { data, error, reload } = useApi("/runs");
  const [open, setOpen] = useState(null);

  // Live refresh while something is running
  useEffect(() => {
    const t = setInterval(reload, 5000);
    return () => clearInterval(t);
  }, [reload]);

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="display-lg">Execution logs</h1>
          <p className="muted">Every run with its status, time, affected students and the input and output of each step.</p>
        </div>
        <Button variant="outline" onClick={reload}>Refresh</Button>
      </div>
      {error && <p className="msg-err">{error}</p>}
      <Card style={{ padding: 0 }}>
        {!data || data.length === 0 ? (
          <div className="empty muted">No runs yet.</div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Workflow</th><th>Trigger</th><th>Students</th><th>Failures</th><th>Started</th><th>Duration</th><th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.map((r) => {
                  const dur = r.finishedAt ? `${Math.max(0, Math.round((new Date(r.finishedAt) - new Date(r.startedAt)) / 100) / 10)}s` : "…";
                  return (
                    <React.Fragment key={r.id}>
                      <tr className="clickable" onClick={() => setOpen(open === r.id ? null : r.id)}>
                        <td className="strong">{r.workflowName} {r.testRun && <Badge>Test</Badge>}</td>
                        <td>{r.triggerType}</td>
                        <td>{r.affected}</td>
                        <td>{r.failures}</td>
                        <td>{fmt(r.startedAt)}</td>
                        <td>{dur}</td>
                        <td><Badge tone={tone(r.status)}>{r.status}</Badge></td>
                      </tr>
                      {open === r.id && (
                        <tr className="detail"><td colSpan={7} style={{ padding: 16 }}><RunDetails id={r.id} /></td></tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
