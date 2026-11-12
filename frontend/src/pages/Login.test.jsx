import React from "react";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp } from "../test/utils";
import Login from "./Login";

function reply(status, body) {
  return { ok: status < 400, status, text: async () => JSON.stringify(body) };
}

describe("Login page", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it("starts empty with no demo shortcuts in production", () => {
    vi.stubEnv("DEV", false);
    vi.stubEnv("VITE_SHOW_DEMO_LOGIN", "");
    renderApp(<Login />);
    expect(screen.getByLabelText("Email")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByRole("button", { name: "Teacher" })).toBeNull();
  });

  it("offers demo shortcuts in development", async () => {
    vi.stubEnv("DEV", true);
    renderApp(<Login />);
    expect(screen.getByLabelText("Email")).toHaveValue("admin@flowedu.local");
    await userEvent.click(screen.getByRole("button", { name: "Teacher" }));
    expect(screen.getByLabelText("Email")).toHaveValue("teacher@flowedu.local");
  });

  it("keeps the shortcuts when the build flag is set", () => {
    vi.stubEnv("DEV", false);
    vi.stubEnv("VITE_SHOW_DEMO_LOGIN", "true");
    renderApp(<Login />);
    expect(screen.getByRole("button", { name: "Student" })).toBeInTheDocument();
  });

  it("shows the server's message when sign-in fails", async () => {
    vi.stubEnv("DEV", false);
    fetch.mockResolvedValue(reply(401, { error: "Invalid email or password" }));
    renderApp(<Login />);
    await userEvent.type(screen.getByLabelText("Email"), "a@b.c");
    await userEvent.type(screen.getByLabelText("Password"), "nope");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByText("Invalid email or password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();
  });

  it("posts the credentials and stores the session on success", async () => {
    vi.stubEnv("DEV", false);
    fetch.mockResolvedValue(reply(200, { token: "jwt", name: "Admin", email: "a@b.c", role: "ADMIN" }));
    renderApp(<Login />);
    await userEvent.type(screen.getByLabelText("Email"), "a@b.c");
    await userEvent.type(screen.getByLabelText("Password"), "secret-pass");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    await waitFor(() => expect(localStorage.getItem("flowedu_auth")).toContain("jwt"));
    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe("/api/auth/login");
    expect(JSON.parse(init.body)).toEqual({ email: "a@b.c", password: "secret-pass" });
  });
});
