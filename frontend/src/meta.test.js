import { describe, expect, it } from "vitest";
import { fmt, tone, triggerSummary } from "./meta";

describe("tone", () => {
  it("maps run statuses to badge tones", () => {
    expect(tone("SUCCESS")).toBe("success");
    expect(tone("FAILED")).toBe("error");
    expect(tone("SIMULATED")).toBe("default");
    expect(tone(undefined)).toBe("default");
  });
});

describe("triggerSummary", () => {
  it("describes event triggers in plain words", () => {
    expect(triggerSummary({ type: "EVENT", event: "ATTENDANCE_UPLOADED" })).toBe("On: attendance uploaded");
  });

  it("shows the cron expression for schedules", () => {
    expect(triggerSummary({ type: "SCHEDULE", cron: "0 0 8 * * MON" })).toBe("Schedule: 0 0 8 * * MON");
  });

  it("names the date source for date triggers", () => {
    expect(triggerSummary({ type: "DATE", daysBefore: 3, dateSource: "EXAM_DATE" })).toBe("3 day(s) before exam");
    expect(triggerSummary({ type: "DATE", daysBefore: 1, dateSource: "ASSIGNMENT_DEADLINE" })).toBe("1 day(s) before deadline");
  });

  it("falls back to a dash for unknown or missing triggers", () => {
    expect(triggerSummary({})).toBe("-");
    expect(triggerSummary()).toBe("-");
  });
});

describe("fmt", () => {
  it("returns a dash for empty values", () => {
    expect(fmt(null)).toBe("-");
    expect(fmt("")).toBe("-");
  });

  it("formats a timestamp as readable text", () => {
    expect(fmt("2026-03-01T10:30:00")).toMatch(/2026/);
  });
});
