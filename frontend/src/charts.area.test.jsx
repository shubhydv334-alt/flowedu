import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { AreaChart } from "./charts";

const data = [
  { label: "Mon", success: 4, failed: 1 },
  { label: "Tue", success: 6, failed: 0 },
  { label: "Wed", success: 2, failed: 3 },
];

describe("AreaChart", () => {
  it("renders the labels and the legend", () => {
    render(<AreaChart data={data} label="Step results" />);
    expect(screen.getByText("Step results")).toBeInTheDocument();
    expect(screen.getByText("Mon")).toBeInTheDocument();
    expect(screen.getByText("Success")).toBeInTheDocument();
    expect(screen.getByText("Failed")).toBeInTheDocument();
  });

  it("draws a data point per series and label", () => {
    const { container } = render(<AreaChart data={data} />);
    expect(container.querySelectorAll("circle")).toHaveLength(data.length * 2);
  });

  it("reveals the counts for a point on hover", async () => {
    const { container } = render(<AreaChart data={data} />);
    await userEvent.hover(container.querySelectorAll("g")[1]);
    expect(container.textContent).toContain("✓6");
  });

  it("produces no invalid coordinates for empty or single-point data", () => {
    const empty = render(<AreaChart />);
    expect(empty.container.innerHTML).not.toContain("NaN");
    const one = render(<AreaChart data={[{ label: "Mon", success: 1, failed: 0 }]} />);
    expect(one.container.innerHTML).not.toContain("NaN");
  });
});
