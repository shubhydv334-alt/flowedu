import React from "react";
import { Button, Card, Badge, Input, Table, TopNav, GradientOrb } from "./ui";

const runs = [
  { id: 1, workflow: "Low Attendance Alert", students: 42, status: "success", time: "10:02" },
  { id: 2, workflow: "Assignment Reminder", students: 18, status: "success", time: "09:30" },
  { id: 3, workflow: "Exam Countdown", students: 120, status: "error", time: "08:15" },
];

const columns = [
  { key: "workflow", header: "Workflow", strong: true },
  { key: "students", header: "Students" },
  { key: "time", header: "Time" },
  {
    key: "status", header: "Status",
    render: (r) => <Badge tone={r.status}>{r.status}</Badge>,
  },
];

export default function StyleGuide() {
  return (
    <>
      <TopNav
        items={[
          { label: "Dashboard", href: "/dashboard" },
          { label: "Workflows", href: "/workflows" },
          { label: "Logs", href: "/logs" },
        ]}
        activePath="/workflows"
        actions={<Button>Try free</Button>}
      />

      <section className="section orb-host">
        <GradientOrb color="mint" size={380} top={-100} left={-80} />
        <GradientOrb color="lavender" size={380} top={-60} right={-80} />
        <div className="container orb-content" style={{ textAlign: "center" }}>
          <h1 className="display-mega">Automate campus<br />communication</h1>
          <p style={{ maxWidth: 520, margin: "24px auto" }}>
            Build workflows that watch attendance, deadlines and exams, and message the right people.
          </p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center" }}>
            <Button>Get started</Button>
            <Button variant="outline">View templates</Button>
          </div>
        </div>
      </section>

      <section className="container" style={{ paddingBottom: 96 }}>
        <h2 className="display-lg" style={{ marginBottom: 24 }}>Components</h2>

        <div className="grid grid-3">
          <Card hover>
            <span className="caption-uppercase">Workflows</span>
            <div className="display-md">12</div>
          </Card>
          <Card hover>
            <span className="caption-uppercase">Messages sent</span>
            <div className="display-md">1,284</div>
          </Card>
          <Card dark>
            <span className="caption-uppercase" style={{ color: "var(--color-on-dark-soft)" }}>Failures</span>
            <div className="display-md" style={{ color: "var(--color-on-dark)" }}>3</div>
          </Card>
        </div>

        <h3 className="display-sm" style={{ margin: "48px 0 16px" }}>Buttons and badges</h3>
        <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
          <Button>Primary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="text">Text link</Button>
          <Button disabled>Disabled</Button>
          <Badge>Draft</Badge>
          <Badge tone="success">Success</Badge>
          <Badge tone="error">Failed</Badge>
        </div>

        <h3 className="display-sm" style={{ margin: "48px 0 16px" }}>Inputs</h3>
        <div className="grid grid-2">
          <Input label="Workflow name" placeholder="Low Attendance Alert" />
          <Input label="Attendance threshold" defaultValue="abc" error="Enter a number" />
        </div>

        <h3 className="display-sm" style={{ margin: "48px 0 16px" }}>Table</h3>
        <Card style={{ padding: 0 }}>
          <Table columns={columns} rows={runs} />
        </Card>
      </section>
    </>
  );
}
