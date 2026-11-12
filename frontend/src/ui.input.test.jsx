import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Input } from "./ui";

describe("Input", () => {
  it("links the label to the field", () => {
    render(<Input label="Roll No" />);
    expect(screen.getByLabelText("Roll No")).toHaveAttribute("id", "roll-no");
  });

  it("prefers an explicit id", () => {
    render(<Input label="Email" id="custom" />);
    expect(screen.getByLabelText("Email")).toHaveAttribute("id", "custom");
  });

  it("shows an error message and marks the field", () => {
    render(<Input label="Email" error="Required" />);
    expect(screen.getByText("Required")).toHaveClass("field-error");
    expect(screen.getByLabelText("Email")).toHaveClass("has-error");
  });

  it("forwards change events", async () => {
    const onChange = vi.fn();
    render(<Input label="Name" onChange={onChange} />);
    await userEvent.type(screen.getByLabelText("Name"), "ab");
    expect(onChange).toHaveBeenCalledTimes(2);
  });
});
