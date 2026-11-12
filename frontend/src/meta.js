// Shared constants for the builder. Keys match the context keys built by WorkflowEngine.

export const VARIABLES = [
  "student.name", "student.email", "student.rollNo", "student.department",
  "student.semester", "student.section", "student.batch",
  "course.name", "course.code", "course.teacher",
  "attendance.percent",
  "assignment.title", "assignment.deadline", "assignment.daysLeft",
  "exam.name", "exam.date", "exam.daysLeft",
  "summary.count", "summary.list",
  "ai.message",
];

export const CONDITION_FIELDS = [
  { key: "attendance.percent", label: "Attendance %" },
  { key: "student.semester", label: "Semester" },
  { key: "student.department", label: "Department" },
  { key: "student.section", label: "Section" },
  { key: "student.batch", label: "Batch" },
  { key: "course.code", label: "Course code" },
  { key: "assignment.submitted", label: "Assignment submitted (true/false)" },
  { key: "assignment.daysLeft", label: "Days left to deadline" },
  { key: "exam.daysLeft", label: "Days left to exam" },
];

export const OPERATORS = ["<", "<=", ">", ">=", "==", "!=", "contains"];

export const EVENTS = [
  { key: "ATTENDANCE_UPLOADED", label: "Attendance uploaded", source: "ATTENDANCE" },
  { key: "ASSIGNMENT_SUBMITTED", label: "Assignment submitted", source: "ASSIGNMENT" },
];

export const DATE_SOURCES = [
  { key: "ASSIGNMENT_DEADLINE", label: "Assignment deadline", source: "ASSIGNMENT" },
  { key: "EXAM_DATE", label: "Exam date", source: "EXAM" },
];

export const SOURCES = ["ATTENDANCE", "ASSIGNMENT", "EXAM", "STUDENT"];

export const CRON_PRESETS = [
  { label: "Mondays 8:00", cron: "0 0 8 * * MON" },
  { label: "Daily 9:00", cron: "0 0 9 * * *" },
  { label: "Every hour", cron: "0 0 * * * *" },
  { label: "Every minute (demo)", cron: "0 * * * * *" },
];

export const STEP_TYPES = [
  { key: "AI", label: "AI message" },
  { key: "EMAIL", label: "Email" },
  { key: "TELEGRAM", label: "Telegram" },
  { key: "WEBHOOK", label: "Webhook" },
];

export const tone = (s) => (s === "SUCCESS" ? "success" : s === "FAILED" ? "error" : "default");

export const fmt = (d) => (d ? new Date(d).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "-");

export function triggerSummary(t = {}) {
  if (t.type === "EVENT") return `On: ${String(t.event || "").replaceAll("_", " ").toLowerCase()}`;
  if (t.type === "SCHEDULE") return `Schedule: ${t.cron}`;
  if (t.type === "DATE") return `${t.daysBefore} day(s) before ${t.dateSource === "EXAM_DATE" ? "exam" : "deadline"}`;
  return "-";
}
