import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./auth";

function Probe() {
  const { user, login, logout, canWrite } = useAuth();
  return (
    <div>
      <span data-testid="who">{user ? `${user.name}:${user.role}` : "anonymous"}</span>
      <span data-testid="write">{String(canWrite)}</span>
      <button onClick={() => login("a@b.c", "pw").catch(() => {})}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  );
}

const mount = () => render(<AuthProvider><Probe /></AuthProvider>);

describe("AuthProvider", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  it("starts anonymous without a stored session", () => {
    mount();
    expect(screen.getByTestId("who")).toHaveTextContent("anonymous");
    expect(screen.getByTestId("write")).toHaveTextContent("false");
  });

  it("restores a stored session", () => {
    localStorage.setItem("flowedu_auth", JSON.stringify({ token: "t", name: "Dr. Rao", role: "TEACHER" }));
    mount();
    expect(screen.getByTestId("who")).toHaveTextContent("Dr. Rao:TEACHER");
    expect(screen.getByTestId("write")).toHaveTextContent("true");
  });

  it("does not grant write access to students", () => {
    localStorage.setItem("flowedu_auth", JSON.stringify({ token: "t", name: "S", role: "STUDENT" }));
    mount();
    expect(screen.getByTestId("write")).toHaveTextContent("false");
  });

  it("stores the session after a successful login", async () => {
    fetch.mockResolvedValue({ ok: true, status: 200, text: async () => JSON.stringify({ token: "jwt", name: "Admin", role: "ADMIN" }) });
    mount();
    await userEvent.click(screen.getByText("login"));
    await waitFor(() => expect(screen.getByTestId("who")).toHaveTextContent("Admin:ADMIN"));
    expect(JSON.parse(localStorage.getItem("flowedu_auth")).token).toBe("jwt");
  });

  it("clears the session on logout", async () => {
    localStorage.setItem("flowedu_auth", JSON.stringify({ token: "t", name: "Admin", role: "ADMIN" }));
    mount();
    await userEvent.click(screen.getByText("logout"));
    expect(screen.getByTestId("who")).toHaveTextContent("anonymous");
    expect(localStorage.getItem("flowedu_auth")).toBeNull();
  });
});
