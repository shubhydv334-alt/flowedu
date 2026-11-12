import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { BarChart } from "./charts";

const data = [{ label: "Mon", value: 2 }, { label: "Tue", value: 8 }, { label: "Wed", value: 0 }];

describe("BarChart", () => {
  it("renders one labelled column per data point", () => {
    render(<BarChart data={data} label="Runs per day" />);
    expect(screen.getByText("Runs per day")).toBeInTheDocument();
    ["Mon", "Tue", "Wed"].forEach((l) => expect(screen.getByText(l)).toBeInTheDocument());
  });

  it("scales bar heights to the largest value", () => {
    const { container } = render(<BarChart data={data} />);
    const heights = [...container.querySelectorAll(".bar-fill")].map((b) => b.style.height);
    expect(heights).toEqual(["25%", "100%", "0%"]);
  });

  it("shows the value in a tooltip on hover", async () => {
    const { container } = render(<BarChart data={data} />);
    expect(container.querySelector(".bar-tooltip")).toBeNull();
    await userEvent.hover(container.querySelectorAll(".bar-chart-col")[1]);
    expect(container.querySelector(".bar-tooltip")).toHaveTextContent("8");
  });

  it("copes with empty data", () => {
    const { container } = render(<BarChart />);
    expect(container.querySelectorAll(".bar-fill")).toHaveLength(0);
  });
});
