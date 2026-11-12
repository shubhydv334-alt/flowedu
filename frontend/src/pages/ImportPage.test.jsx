import React from "react";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderApp, signInAs } from "../test/utils";
import ImportPage from "./ImportPage";

const reply = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body) });
const csv = () => new File(["rollNo,courseCode,percent\nCSE1,CS501,70"], "attendance.csv", { type: "text/csv" });

const previewBody = {
  type: "attendance", total: 2, valid: 1, invalid: 1,
  errors: [{ line: 3, message: "Unknown course CS999" }],
  preview: [{ rollNo: "CSE1", courseCode: "CS501", percent: "70" }],
};

describe("ImportPage", () => {
  beforeEach(() => {
    signInAs("TEACHER");
    vi.stubGlobal("fetch", vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it("asks for a file before previewing", async () => {
    renderApp(<ImportPage />);
    await userEvent.click(screen.getByRole("button", { name: "Preview" }));
    expect(screen.getByText("Choose a CSV file first")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("describes the columns of the selected import type", async () => {
    renderApp(<ImportPage />);
    expect(screen.getByText(/rollNo,courseCode,percent,date/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Students" }));
    expect(screen.getByText(/telegramChatId,department/)).toBeInTheDocument();
  });

  it("previews valid rows and row errors without importing", async () => {
    fetch.mockResolvedValue(reply(200, previewBody));
    const { container } = renderApp(<ImportPage />);
    await userEvent.upload(container.querySelector('input[type="file"]'), csv());
    await userEvent.click(screen.getByRole("button", { name: "Preview" }));
    expect(await screen.findByText("Line 3: Unknown course CS999")).toBeInTheDocument();
    expect(screen.getByText("1 with errors")).toBeInTheDocument();
    expect(fetch.mock.calls[0][0]).toBe("/api/import/attendance?preview=true");
    expect(screen.getByRole("button", { name: "Import 1 rows" })).toBeEnabled();
  });

  it("imports after a preview and reports the result", async () => {
    fetch.mockResolvedValueOnce(reply(200, previewBody)).mockResolvedValueOnce(reply(200, { ...previewBody, imported: 1 }));
    const { container } = renderApp(<ImportPage />);
    await userEvent.upload(container.querySelector('input[type="file"]'), csv());
    await userEvent.click(screen.getByRole("button", { name: "Preview" }));
    await userEvent.click(await screen.findByRole("button", { name: "Import 1 rows" }));
    expect(await screen.findByText(/Imported 1 rows/)).toBeInTheDocument();
    expect(fetch.mock.calls[1][0]).toBe("/api/import/attendance?preview=false");
  });

  it("shows server errors", async () => {
    fetch.mockResolvedValue(reply(400, { error: "The file is empty" }));
    const { container } = renderApp(<ImportPage />);
    await userEvent.upload(container.querySelector('input[type="file"]'), csv());
    await userEvent.click(screen.getByRole("button", { name: "Preview" }));
    expect(await screen.findByText("The file is empty")).toBeInTheDocument();
  });
});
