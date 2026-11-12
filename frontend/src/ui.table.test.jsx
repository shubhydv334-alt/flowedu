import React from "react";
import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Table } from "./ui";

const columns = [
  { key: "name", header: "Name", strong: true },
  { key: "score", header: "Score", render: (r) => `${r.score}%` },
];

describe("Table", () => {
  it("renders headers and rows", () => {
    render(<Table columns={columns} rows={[{ id: 1, name: "Diya", score: 91 }, { id: 2, name: "Rohan", score: 74 }]} />);
    expect(screen.getAllByRole("columnheader").map((h) => h.textContent)).toEqual(["Name", "Score"]);
    const rows = screen.getAllByRole("row");
    expect(rows).toHaveLength(3);
    expect(within(rows[1]).getByText("Diya")).toHaveClass("strong");
    expect(within(rows[2]).getByText("74%")).toBeInTheDocument();
  });

  it("falls back to the row index when the key field is missing", () => {
    render(<Table columns={columns} rows={[{ name: "A", score: 1 }, { name: "B", score: 2 }]} />);
    expect(screen.getAllByRole("row")).toHaveLength(3);
  });

  it("renders only headers for empty data", () => {
    render(<Table columns={columns} rows={[]} />);
    expect(screen.getAllByRole("row")).toHaveLength(1);
  });
});
