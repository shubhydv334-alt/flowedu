import React from "react";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Templates from "./Templates";

const templates = [
  {
    key: "low-attendance", name: "Low Attendance Alert", badge: "Attendance",
    description: "Warn students below the threshold.",
    trigger: { type: "EVENT", event: "ATTENDANCE_UPLOADED" },
    steps: [{ type: "AI" }, { type: "EMAIL" }],
  },
];

const reply = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body) });

function mount() {
  return renderApp(
    <Routes>
      <Route path="/templates" element={<Templates />} />
      <Route path="/workflows/:id" element={<p>builder opened</p>} />
    </Routes>,
    { route: "/templates" }
  );
}

describe("Templates page", () => {
  beforeEach(() => {
    signInAs("TEACHER");
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("lists templates with their trigger and step summary", async () => {
    fetch.mockResolvedValue(reply(200, templates));
    mount();
    expect(await screen.findByText("Low Attendance Alert")).toBeInTheDocument();
    expect(screen.getByText(/On: attendance uploaded · AI → EMAIL/)).toBeInTheDocument();
  });

  it("creates a workflow and opens the builder", async () => {
    fetch.mockResolvedValueOnce(reply(200, templates)).mockResolvedValueOnce(reply(200, { id: 7 }));
    mount();
    await userEvent.click(await screen.findByRole("button", { name: "Use template" }));
    expect(await screen.findByText("builder opened")).toBeInTheDocument();
    expect(fetch.mock.calls[1][0]).toBe("/api/templates/low-attendance/use");
    expect(fetch.mock.calls[1][1].method).toBe("POST");
  });

  it("shows an error when creating the workflow fails", async () => {
    fetch.mockResolvedValueOnce(reply(200, templates)).mockResolvedValueOnce(reply(500, { error: "Unexpected server error" }));
    mount();
    await userEvent.click(await screen.findByRole("button", { name: "Use template" }));
    expect(await screen.findByText("Unexpected server error")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Use template" })).toBeEnabled();
  });

  it("shows the load error when templates cannot be fetched", async () => {
    fetch.mockResolvedValue(reply(403, { error: "You do not have permission to do this" }));
    mount();
    expect(await screen.findByText("You do not have permission to do this")).toBeInTheDocument();
  });
});
