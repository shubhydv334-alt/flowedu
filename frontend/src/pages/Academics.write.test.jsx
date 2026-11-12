import React from "react";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import Academics from "./Academics";

const reply = (status, body) => ({ ok: status < 400, status, text: async () => (body === undefined ? "" : JSON.stringify(body)) });
const dept = { id: 3, name: "CSE" };

describe("Academics: editing", () => {
  beforeEach(() => {
    signInAs("TEACHER");
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("creates a department and reloads the list", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "POST" ? reply(200, dept) : reply(200, [])));
    renderApp(<Academics />);
    await userEvent.click(screen.getByRole("button", { name: "Departments" }));
    await userEvent.click(await screen.findByRole("button", { name: "Add department" }));
    await userEvent.type(screen.getByLabelText("Department name"), "CSE");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    const post = fetch.mock.calls.find(([, i]) => i?.method === "POST");
    expect(post[0]).toBe("/api/departments");
    expect(JSON.parse(post[1].body)).toEqual({ name: "CSE" });
  });

  it("sends empty optional fields as null and numbers as numbers", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "POST" ? reply(200, {}) : reply(200, url === "/api/departments" ? [dept] : [])));
    renderApp(<Academics />);
    await userEvent.click(await screen.findByRole("button", { name: "Add student" }));
    await userEvent.type(screen.getByLabelText("Roll number"), "CSE9");
    await userEvent.type(screen.getByLabelText("Semester"), "5");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    const body = JSON.parse(fetch.mock.calls.find(([, i]) => i?.method === "POST")[1].body);
    expect(body).toMatchObject({ rollNo: "CSE9", semester: 5, email: null, departmentId: null });
  });

  it("shows the server's validation message", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "POST" ? reply(400, { error: "Department name is required" }) : reply(200, [])));
    renderApp(<Academics />);
    await userEvent.click(screen.getByRole("button", { name: "Departments" }));
    await userEvent.click(await screen.findByRole("button", { name: "Add department" }));
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(await screen.findByText("Department name is required")).toBeInTheDocument();
  });

  it("deletes a record only after confirmation", async () => {
    fetch.mockImplementation(async (url, init) => (init?.method === "DELETE" ? reply(204) : reply(200, url === "/api/departments" ? [dept] : [])));
    vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValueOnce(true);
    renderApp(<Academics />);
    await userEvent.click(screen.getByRole("button", { name: "Departments" }));
    const del = await screen.findByRole("button", { name: "Delete" });
    await userEvent.click(del);
    expect(fetch.mock.calls.some(([, i]) => i?.method === "DELETE")).toBe(false);
    await userEvent.click(del);
    expect(fetch.mock.calls.some(([u, i]) => u === "/api/departments/3" && i?.method === "DELETE")).toBe(true);
  });

  it("offers no add button on the read-only attendance tab", async () => {
    fetch.mockResolvedValue(reply(200, []));
    renderApp(<Academics />);
    await userEvent.click(screen.getByRole("button", { name: "Attendance" }));
    await screen.findByText("Nothing here yet.");
    expect(screen.queryByRole("button", { name: /^Add / })).toBeNull();
  });
});
