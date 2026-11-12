import React from "react";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Workflows from "./Workflows";

const workflows = [
  {
    id: 1, name: "Low attendance", enabled: true, lastRunAt: null,
    trigger: { type: "EVENT", event: "ATTENDANCE_UPLOADED" }, steps: [{ type: "AI" }, { type: "EMAIL" }],
  },
  {
    id: 2, name: "Exam reminder", enabled: false, lastRunAt: null,
    trigger: { type: "DATE", daysBefore: 3, dateSource: "EXAM_DATE" }, steps: [{ type: "TELEGRAM" }],
  },
];

const reply = (status, body) => ({ ok: status < 400, status, text: async () => (body === undefined ? "" : JSON.stringify(body)) });

describe("Workflows page", () => {
  beforeEach(() => {
    signInAs("ADMIN");
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("shows an empty state", async () => {
    fetch.mockResolvedValue(reply(200, []));
    renderApp(<Workflows />);
    expect(await screen.findByText(/No workflows yet/)).toBeInTheDocument();
  });

  it("lists workflows with trigger, actions and status", async () => {
    fetch.mockResolvedValue(reply(200, workflows));
    renderApp(<Workflows />);
    const row = (await screen.findByText("Low attendance")).closest("tr");
    expect(within(row).getByText("On: attendance uploaded")).toBeInTheDocument();
    expect(within(row).getByText("AI → EMAIL")).toBeInTheDocument();
    expect(within(row).getByText("Active")).toBeInTheDocument();
    const other = screen.getByText("Exam reminder").closest("tr");
    expect(within(other).getByText("Disabled")).toBeInTheDocument();
    expect(within(other).getByRole("button", { name: "Enable" })).toBeInTheDocument();
  });

  it("toggles a workflow and reloads the list", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "POST" ? reply(200, {}) : reply(200, workflows)));
    renderApp(<Workflows />);
    const row = (await screen.findByText("Exam reminder")).closest("tr");
    await userEvent.click(within(row).getByRole("button", { name: "Enable" }));
    expect(await screen.findByText("Enabled")).toBeInTheDocument();
    expect(fetch.mock.calls.some(([u, i]) => u === "/api/workflows/2/toggle" && i.method === "POST")).toBe(true);
  });

  it("only deletes after confirmation", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "DELETE" ? reply(204) : reply(200, workflows)));
    const confirm = vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValueOnce(true);
    renderApp(<Workflows />);
    const row = (await screen.findByText("Low attendance")).closest("tr");
    await userEvent.click(within(row).getByRole("button", { name: "Delete" }));
    expect(fetch.mock.calls.some(([, i]) => i?.method === "DELETE")).toBe(false);
    await userEvent.click(within(row).getByRole("button", { name: "Delete" }));
    expect(await screen.findByText("Deleted")).toBeInTheDocument();
    expect(confirm).toHaveBeenCalledTimes(2);
  });

  it("reports action failures", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "POST" ? reply(500, { error: "Unexpected server error" }) : reply(200, workflows)));
    renderApp(<Workflows />);
    const row = (await screen.findByText("Low attendance")).closest("tr");
    await userEvent.click(within(row).getByRole("button", { name: "Run now" }));
    expect(await screen.findByText("Error: Unexpected server error")).toBeInTheDocument();
  });
});
