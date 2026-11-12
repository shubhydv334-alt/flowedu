import React from "react";
import { Link } from "react-router-dom";
import { Badge, Card, Table, GradientOrb } from "../ui";
import { BarChart, DonutChart, AreaChart } from "../charts";
import { useApi } from "../api";
import { useAuth } from "../auth";
import { fmt, tone } from "../meta";
import "../charts.css";

export default function Dashboard() {
  const { data, error, loading } = useApi("/dashboard");
  const { user, canWrite } = useAuth();

  const stats = data && [
    ["Workflows", `${data.workflows}`, `${data.activeWorkflows} active`],
    ["Messages sent", data.messagesSent, "email, Telegram, webhook"],
    ["Failures", data.failures, "failed steps"],
    ["Active students", data.activeStudents, "in the system"],
  ];

  const columns = [
    { key: "workflowName", header: "Workflow", strong: true },
    { key: "triggerType", header: "Trigger" },
    { key: "affected", header: "Students" },
    { key: "startedAt", header: "Started", render: (r) => fmt(r.startedAt) },
    { key: "status", header: "Status", render: (r) => <Badge tone={tone(r.status)}>{r.status}</Badge> },
  ];

  // Prepare chart data from API response
  const runsPerDay = data?.runsPerDay || [];
  const messagesByType = data?.messagesByType || {};
  const statusDistribution = data?.statusDistribution || {};
  const stepsByDay = data?.stepsByDay || [];

  const donutData = Object.entries(messagesByType).map(([label, value]) => ({
    label: label.charAt(0) + label.slice(1).toLowerCase(),
    value,
  }));

  const statusDonutData = Object.entries(statusDistribution).map(([label, value]) => ({
    label: label.charAt(0) + label.slice(1).toLowerCase(),
    value,
  }));

  const hasChartData = runsPerDay.length > 0 || donutData.length > 0 || statusDonutData.length > 0 || stepsByDay.length > 0;

  return (
    <>
      <div className="orb-host" style={{ borderRadius: 24, padding: "40px 32px", marginBottom: 24 }}>
        <GradientOrb color="mint" size={320} top={-120} left={-60} />
        <GradientOrb color="sky" size={320} top={-100} right={-40} />
        <div className="orb-content">
          <h1 className="display-lg">Welcome, {user.name.split(" ")[0]}</h1>
          <p style={{ marginTop: 8 }}>Here is what your workflows have been doing.</p>
        </div>
      </div>

      {error && <p className="msg-err">{error}</p>}
      {loading && !data && <p className="muted">Loading…</p>}

      {data && (
        <>
          <div className="grid grid-4">
            {stats.map(([label, value, hint]) => (
              <Card key={label} hover dark={label === "Failures" && data.failures > 0}>
                <span className="caption-uppercase" style={{ color: label === "Failures" && data.failures > 0 ? "var(--color-on-dark-soft)" : "var(--color-muted)" }}>
                  {label}
                </span>
                <div className="display-lg" style={{ margin: "8px 0 4px", color: label === "Failures" && data.failures > 0 ? "var(--color-on-dark)" : undefined }}>{value}</div>
                <span className="caption muted">{hint}</span>
              </Card>
            ))}
          </div>

          {/* ─────── Charts Section ─────── */}
          {hasChartData && (
            <div className="charts-section">
              <div className="page-head">
                <h2 className="display-md">Analytics</h2>
              </div>
              <div className="charts-grid">
                {/* Bar Chart — Workflow runs per day */}
                <div className="chart-card">
                  <BarChart
                    data={runsPerDay}
                    height={180}
                    label="Workflow runs (last 7 days)"
                  />
                </div>

                {/* Donut Chart — Messages by channel */}
                <div className="chart-card">
                  <DonutChart
                    data={donutData}
                    size={160}
                    label="Messages by channel"
                  />
                </div>

                {/* Area Chart — Step outcomes over time */}
                <div className="chart-card">
                  <AreaChart
                    data={stepsByDay}
                    height={180}
                    label="Step outcomes (last 7 days)"
                  />
                </div>

                {/* Donut Chart — Run status distribution */}
                <div className="chart-card">
                  <DonutChart
                    data={statusDonutData}
                    size={160}
                    label="Run status distribution"
                  />
                </div>
              </div>
            </div>
          )}

          <div className="page-head" style={{ marginTop: 48 }}>
            <h2 className="display-md">Recent runs</h2>
            {canWrite && <Link className="btn btn-outline btn-sm" to="/logs">All logs</Link>}
          </div>
          <Card style={{ padding: 0 }}>
            {data.recentRuns.length === 0 ? (
              <div className="empty muted">No runs yet. Use a template and upload attendance to see one.</div>
            ) : (
              <Table columns={columns} rows={data.recentRuns} />
            )}
          </Card>
        </>
      )}
    </>
  );
}
