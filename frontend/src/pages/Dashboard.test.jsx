import React from "react";
import { screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Dashboard from "./Dashboard";

const reply = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body) });

const stats = {
  workflows: 4, activeWorkflows: 2, messagesSent: 31, failures: 0, activeStudents: 120,
  recentRuns: [{ id: 1, workflowName: "Exam countdown", triggerType: "DATE", affected: 12, startedAt: "2026-03-01T09:00:00", status: "SUCCESS" }],
  runsPerDay: [{ label: "Mon", value: 3 }],
  messagesByType: { EMAIL: 20, TELEGRAM: 11 },
  statusDistribution: { SUCCESS: 5 },
  stepsByDay: [{ label: "Mon", success: 5, failed: 0 }],
};

describe("Dashboard", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("greets the user by first name and shows the KPI cards", async () => {
    signInAs("ADMIN", "Priya Nair");
    fetch.mockResolvedValue(reply(200, stats));
    renderApp(<Dashboard />);
    expect(screen.getByText("Welcome, Priya")).toBeInTheDocument();
    const messages = (await screen.findByText("Messages sent")).closest(".card");
    expect(messages).toHaveTextContent("31");
    expect(screen.getByText("2 active")).toBeInTheDocument();
    expect(screen.getByText("Active students").closest(".card")).toHaveTextContent("120");
  });

  it("lists recent runs and links to all logs for staff", async () => {
    signInAs("TEACHER");
    fetch.mockResolvedValue(reply(200, stats));
    renderApp(<Dashboard />);
    expect(await screen.findByText("Exam countdown")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "All logs" })).toBeInTheDocument();
  });

  it("hides the logs link from students", async () => {
    signInAs("STUDENT");
    fetch.mockResolvedValue(reply(200, stats));
    renderApp(<Dashboard />);
    await screen.findByText("Exam countdown");
    expect(screen.queryByRole("link", { name: "All logs" })).toBeNull();
  });

  it("capitalises chart legend labels", async () => {
    signInAs("ADMIN");
    fetch.mockResolvedValue(reply(200, stats));
    renderApp(<Dashboard />);
    expect(await screen.findByText("Email")).toBeInTheDocument();
    expect(screen.getByText("Telegram")).toBeInTheDocument();
  });

  it("shows the error when the dashboard cannot load", async () => {
    signInAs("ADMIN");
    fetch.mockResolvedValue(reply(500, { error: "Unexpected server error" }));
    renderApp(<Dashboard />);
    expect(await screen.findByText("Unexpected server error")).toBeInTheDocument();
  });
});
