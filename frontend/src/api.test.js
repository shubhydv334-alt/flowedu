import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, getAuth, setAuth } from "./api";

function jsonResponse(status, body) {
  return { ok: status >= 200 && status < 300, status, text: async () => (body === undefined ? "" : JSON.stringify(body)) };
}

describe("api client", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends the bearer token and JSON body", async () => {
    setAuth({ token: "abc" });
    fetch.mockResolvedValue(jsonResponse(200, { ok: true }));
    const data = await api("/workflows", { method: "POST", body: { name: "x" } });
    expect(data).toEqual({ ok: true });
    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe("/api/workflows");
    expect(init.method).toBe("POST");
    expect(init.headers.Authorization).toBe("Bearer abc");
    expect(init.headers["Content-Type"]).toBe("application/json");
    expect(init.body).toBe(JSON.stringify({ name: "x" }));
  });

  it("surfaces the server's error message", async () => {
    fetch.mockResolvedValue(jsonResponse(400, { error: "Workflow name is required" }));
    await expect(api("/workflows", { method: "POST", body: {} })).rejects.toThrow("Workflow name is required");
  });

  it("falls back to the status code when the body has no message", async () => {
    fetch.mockResolvedValue(jsonResponse(500));
    await expect(api("/workflows")).rejects.toThrow("Request failed (500)");
  });

  it("clears the session and redirects after a 401 for a signed-in user", async () => {
    setAuth({ token: "expired" });
    const location = { href: "/workflows" };
    vi.stubGlobal("location", location);
    fetch.mockResolvedValue(jsonResponse(401));
    await expect(api("/workflows")).rejects.toThrow("Session expired");
    expect(getAuth()).toBeNull();
    expect(location.href).toBe("/login");
  });

  it("does not redirect on a failed login attempt", async () => {
    const location = { href: "/login" };
    vi.stubGlobal("location", location);
    fetch.mockResolvedValue(jsonResponse(401, { error: "Invalid email or password" }));
    await expect(api("/auth/login", { method: "POST", body: {} })).rejects.toThrow("Invalid email or password");
    expect(location.href).toBe("/login");
  });
});
