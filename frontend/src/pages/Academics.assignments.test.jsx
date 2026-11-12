import React from "react";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Academics from "./Academics";

const reply = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body) });
const assignment = { id: 9, title: "DBMS Lab Report", course: { name: "Database Systems", code: "CS502" }, deadline: "2026-03-05T17:00:00" };
const pending = [{ id: 4, rollNo: "CSE3", name: "Rohan Verma", email: "r@example.edu" }];

describe("Academics: assignment submissions", () => {
  beforeEach(() => {
    signInAs("TEACHER");
    vi.stubGlobal("fetch", vi.fn(async (url, init) => {
      if (url === "/api/assignments") return reply(200, [assignment]);
      if (url === "/api/assignments/9/not-submitted") return reply(200, pending);
      if (url === "/api/assignments/9/submit" && init?.method === "POST") return reply(200, {});
      return reply(200, []);
    }));
  });
  afterEach(() => vi.unstubAllGlobals());

  async function openPending() {
    renderApp(<Academics />);
    await userEvent.click(screen.getByRole("button", { name: "Assignments" }));
    await userEvent.click(await screen.findByRole("button", { name: "Not submitted" }));
  }

  it("lists students who have not submitted", async () => {
    await openPending();
    expect(await screen.findByText("Not submitted: DBMS Lab Report")).toBeInTheDocument();
    expect(screen.getByText("Rohan Verma")).toBeInTheDocument();
  });

  it("marks a student as submitted", async () => {
    await openPending();
    await userEvent.click(await screen.findByRole("button", { name: "Mark submitted" }));
    const post = fetch.mock.calls.find(([u, i]) => u === "/api/assignments/9/submit" && i?.method === "POST");
    expect(JSON.parse(post[1].body)).toEqual({ studentId: 4 });
  });

  it("closes the panel", async () => {
    await openPending();
    await screen.findByText("Not submitted: DBMS Lab Report");
    await userEvent.click(screen.getByRole("button", { name: "Close" }));
    expect(screen.queryByText("Not submitted: DBMS Lab Report")).toBeNull();
  });
});
