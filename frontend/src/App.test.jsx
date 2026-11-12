import React from "react";
import { screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { renderApp, signInAs } from "./test/utils";

const reply = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body) });
const dashboard = {
  workflows: 0, activeWorkflows: 0, messagesSent: 0, failures: 0, activeStudents: 0,
  recentRuns: [], runsPerDay: [], messagesByType: {}, statusDistribution: {}, stepsByDay: [],
};

describe("App routing", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubEnv("DEV", false);
    vi.stubGlobal("fetch", vi.fn(async (url) => reply(200, url === "/api/dashboard" ? dashboard : [])));
  });
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it("sends anonymous visitors to the login page", () => {
    renderApp(<App />, { route: "/workflows" });
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });

  it("shows the staff menu to teachers", async () => {
    signInAs("TEACHER", "Dr. Rao");
    renderApp(<App />, { route: "/templates" });
    expect(await screen.findByRole("link", { name: "Workflows" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Logs" })).toBeInTheDocument();
    expect(screen.getByText(/Dr\. Rao · teacher/)).toBeInTheDocument();
  });

  it("keeps students out of staff pages", async () => {
    signInAs("STUDENT", "Aarav Sharma");
    renderApp(<App />, { route: "/workflows" });
    expect(await screen.findByText("Welcome, Aarav")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Workflows" })).toBeNull();
    expect(screen.getByRole("link", { name: "Academics" })).toBeInTheDocument();
  });

  it("redirects signed-in users away from the login page", async () => {
    signInAs("ADMIN", "Admin User");
    renderApp(<App />, { route: "/login" });
    expect(await screen.findByText("Welcome, Admin")).toBeInTheDocument();
  });

  it("falls back to the dashboard for unknown paths", async () => {
    signInAs("ADMIN", "Admin User");
    renderApp(<App />, { route: "/does-not-exist" });
    expect(await screen.findByText("Welcome, Admin")).toBeInTheDocument();
  });
});
