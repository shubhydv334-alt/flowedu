import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { TopNav } from "./ui";

const items = [{ label: "Dashboard", href: "/dashboard" }, { label: "Logs", href: "/logs" }];

describe("TopNav", () => {
  it("renders the brand and links and marks the active one", () => {
    render(<TopNav brand="FlowEdu" items={items} activePath="/logs" />);
    expect(screen.getByText("FlowEdu")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Logs" })).toHaveClass("active");
    expect(screen.getByRole("link", { name: "Dashboard" })).not.toHaveClass("active");
  });

  it("lets the caller render links", () => {
    render(<TopNav items={items} renderLink={(it, cls) => <span key={it.href} className={cls}>{it.label}!</span>} />);
    expect(screen.getByText("Dashboard!")).toBeInTheDocument();
  });

  it("toggles the mobile menu", async () => {
    const { container } = render(<TopNav items={items} />);
    const nav = container.querySelector("nav");
    expect(nav).not.toHaveClass("open");
    await userEvent.click(screen.getByLabelText("Menu"));
    expect(nav).toHaveClass("open");
  });

  it("renders action slots", () => {
    render(<TopNav items={items} actions={<button>Sign out</button>} />);
    expect(screen.getByRole("button", { name: "Sign out" })).toBeInTheDocument();
  });
});
