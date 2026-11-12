import React from "react";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Academics from "./Academics";

const reply = (status, body) => ({ ok: status < 400, status, text: async () => (body === undefined ? "" : JSON.stringify(body)) });

const students = [
  { id: 1, rollNo: "CSE1", name: "Aarav Sharma", department: { name: "CSE" }, semester: 5, section: "A", batch: "2021", email: "a@example.edu" },
  { id: 2, rollNo: "ECE1", name: "Ishaan Gupta", department: { name: "ECE" }, semester: 5, section: "A", batch: "2021", email: "i@example.edu" },
];

describe("Academics: browsing", () => {
  beforeEach(() => {
    signInAs("STUDENT");
    vi.stubGlobal("fetch", vi.fn(async (url) => (url === "/api/students" ? reply(200, students) : reply(200, []))));
  });
  afterEach(() => vi.unstubAllGlobals());

  it("opens on the students tab and lists records", async () => {
    renderApp(<Academics />);
    expect(await screen.findByText("Aarav Sharma")).toBeInTheDocument();
    expect(screen.getByText("Ishaan Gupta")).toBeInTheDocument();
  });

  it("filters by search text", async () => {
    renderApp(<Academics />);
    await screen.findByText("Aarav Sharma");
    await userEvent.type(screen.getByPlaceholderText("Search…"), "ishaan");
    expect(screen.queryByText("Aarav Sharma")).toBeNull();
    expect(screen.getByText("Ishaan Gupta")).toBeInTheDocument();
  });

  it("filters by department pill", async () => {
    renderApp(<Academics />);
    await screen.findByText("Aarav Sharma");
    await userEvent.click(screen.getByRole("button", { name: "ECE" }));
    expect(screen.queryByText("Aarav Sharma")).toBeNull();
    expect(screen.getByText("Ishaan Gupta")).toBeInTheDocument();
  });

  it("switches tabs and loads that collection", async () => {
    renderApp(<Academics />);
    await userEvent.click(screen.getByRole("button", { name: "Exams" }));
    expect(await screen.findByText("Nothing here yet.")).toBeInTheDocument();
    expect(fetch.mock.calls.map(([u]) => u)).toContain("/api/exams");
  });

  it("gives students no way to add or delete", async () => {
    renderApp(<Academics />);
    await screen.findByText("Aarav Sharma");
    expect(screen.queryByRole("button", { name: /^Add / })).toBeNull();
    expect(screen.queryByRole("button", { name: "Delete" })).toBeNull();
  });
});
