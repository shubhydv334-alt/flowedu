import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../auth";

/** Renders a tree inside a router and the auth provider, starting at the given path. */
export function renderApp(ui, { route = "/" } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>{ui}</AuthProvider>
    </MemoryRouter>
  );
}

/** Stores a signed-in user the way the login flow does. */
export function signInAs(role = "ADMIN", name = "Test User") {
  localStorage.setItem("flowedu_auth", JSON.stringify({ token: "t", name, email: "t@example.edu", role }));
}
