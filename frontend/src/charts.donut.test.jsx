import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DonutChart } from "./charts";

describe("DonutChart", () => {
  it("shows the total in the centre and one legend row per slice", () => {
    render(<DonutChart data={[{ label: "Email", value: 6 }, { label: "Telegram", value: 4 }]} label="Channels" />);
    expect(screen.getByText("Channels")).toBeInTheDocument();
    expect(screen.getByText("10")).toBeInTheDocument();
    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("Telegram")).toBeInTheDocument();
  });

  it("draws one arc per slice and honours custom colours", () => {
    const { container } = render(<DonutChart data={[{ label: "A", value: 1, color: "#123456" }, { label: "B", value: 3 }]} />);
    const arcs = container.querySelectorAll("path");
    expect(arcs).toHaveLength(2);
    expect(arcs[0]).toHaveAttribute("stroke", "#123456");
  });

  it("does not divide by zero when every value is zero", () => {
    const { container } = render(<DonutChart data={[{ label: "A", value: 0 }]} />);
    expect(container.querySelector("svg")).toBeInTheDocument();
    expect(container.innerHTML).not.toContain("NaN");
  });
});
