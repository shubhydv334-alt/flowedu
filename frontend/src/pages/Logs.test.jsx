import React from "react";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Logs from "./Logs";

const run = {
  id: 5, workflowName: "Low attendance", testRun: true, triggerType: "MANUAL", affected: 3, failures: 1,
  startedAt: "2026-03-01T10:00:00", finishedAt: "2026-03-01T10:00:02", status: "PARTIAL",
};

const detail = {
  run: { id: 5, error: null },
  steps: [{
    id: 1, stepIndex: 1, stepType: "EMAIL", studentName: "Aarav", status: "FAILED", attempts: 3,
    loggedAt: "2026-03-01T10:00:01", error: "No recipient email address",
    inputJson: '{"to":""}', outputJson: null,
  }],
};

const reply = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body) });

describe("Logs page", () => {
  beforeEach(() => {
    signInAs("ADMIN");
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("shows an empty state", async () => {
    fetch.mockResolvedValue(reply(200, []));
    renderApp(<Logs />);
    expect(await screen.findByText("No runs yet.")).toBeInTheDocument();
  });

  it("lists runs with counts, duration and status", async () => {
    fetch.mockResolvedValue(reply(200, [run]));
    renderApp(<Logs />);
    const row = (await screen.findByText(/Low attendance/)).closest("tr");
    expect(within(row).getByText("Test")).toBeInTheDocument();
    expect(within(row).getByText("2s")).toBeInTheDocument();
    expect(within(row).getByText("PARTIAL")).toBeInTheDocument();
  });

  it("expands a run to show step input, output and errors", async () => {
    fetch.mockImplementation(async (url) => (url === "/api/runs/5" ? reply(200, detail) : reply(200, [run])));
    renderApp(<Logs />);
    await userEvent.click((await screen.findByText(/Low attendance/)).closest("tr"));
    expect(await screen.findByText("No recipient email address")).toBeInTheDocument();
    expect(screen.getByText("3 attempts · ", { exact: false })).toBeInTheDocument();
    expect(screen.getByText(/"to": ""/)).toBeInTheDocument();
  });

  it("explains runs that matched nobody", async () => {
    fetch.mockImplementation(async (url) => (url === "/api/runs/5" ? reply(200, { run: { id: 5 }, steps: [] }) : reply(200, [run])));
    renderApp(<Logs />);
    await userEvent.click((await screen.findByText(/Low attendance/)).closest("tr"));
    expect(await screen.findByText(/No students matched the conditions/)).toBeInTheDocument();
  });

  it("refreshes on demand", async () => {
    fetch.mockResolvedValue(reply(200, [run]));
    renderApp(<Logs />);
    await screen.findByText(/Low attendance/);
    const before = fetch.mock.calls.length;
    await userEvent.click(screen.getByRole("button", { name: "Refresh" }));
    expect(fetch.mock.calls.length).toBe(before + 1);
  });
});
