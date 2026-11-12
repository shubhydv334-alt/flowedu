import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Badge, Card } from "./ui";

describe("Card", () => {
  it("renders children inside a card", () => {
    render(<Card data-testid="c">content</Card>);
    expect(screen.getByTestId("c")).toHaveClass("card");
    expect(screen.getByText("content")).toBeInTheDocument();
  });

  it("supports the dark and hover modifiers", () => {
    render(<Card dark hover data-testid="c">x</Card>);
    expect(screen.getByTestId("c")).toHaveClass("card-dark", "card-hover");
  });
});

describe("Badge", () => {
  it("uses no tone class by default", () => {
    render(<Badge>Idle</Badge>);
    expect(screen.getByText("Idle")).toHaveClass("badge");
    expect(screen.getByText("Idle").className).not.toMatch(/badge-/);
  });

  it("adds the tone class for success and error", () => {
    render(<><Badge tone="success">ok</Badge><Badge tone="error">bad</Badge></>);
    expect(screen.getByText("ok")).toHaveClass("badge-success");
    expect(screen.getByText("bad")).toHaveClass("badge-error");
  });
});
