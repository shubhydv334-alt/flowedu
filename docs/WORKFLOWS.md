# Workflow Guide

A workflow is a **trigger**, optional **conditions** and an ordered list of
**actions**. Each live run records the outcome of every step in the run history.

## Triggers

| Type | Fires when | Configuration |
| --- | --- | --- |
| `EVENT` | Something happens in the app | `event`: `ATTENDANCE_UPLOADED` or `ASSIGNMENT_SUBMITTED` |
| `SCHEDULE` | A cron expression matches the current minute | Spring cron, six fields (`0 0 9 * * MON-FRI`) |
| `DATE` | The daily date check runs (09:00 by default, `flowedu.date-trigger-cron`) | The workflow selects matching rows itself, for example "N days before a deadline" |

Only enabled workflows are considered. Schedule triggers are checked every minute.

## Conditions

Each condition compares a context value with a literal.

* Numeric comparison: `<`, `<=`, `>`, `>=`, `==`, `!=`
* Text comparison: `==`, `!=` (case-insensitive) and `contains`

Example: `attendance.percent < 75` sends the alert only to students below 75%.

## Actions

| Type | What it does |
| --- | --- |
| `EMAIL` | Sends an email to the student, the course teacher or a custom address |
| `TELEGRAM` | Sends a Telegram message to the student, teacher or a custom chat |
| `WEBHOOK` | Calls an HTTP endpoint. Private and loopback addresses are blocked unless `ALLOW_PRIVATE_WEBHOOKS=true` |
| `AI` | Summarizes or rewrites text; later steps can use the result as `ai.message` |

## Template variables

Use `{{name}}` in messages and subjects. Add a fallback after a pipe when a value
may be empty: `{{student.name|there}}`.

| Group | Variables |
| --- | --- |
| Student | `student.name`, `student.email`, `student.rollNo`, `student.department`, `student.semester`, `student.section`, `student.batch`, `student.telegramChatId` |
| Course | `course.name`, `course.code`, `course.teacher`, `course.teacherEmail`, `course.teacherTelegram` |
| Attendance | `attendance.percent` |
| Assignment | `assignment.title`, `assignment.deadline`, `assignment.submitted`, `assignment.daysLeft` |
| Exam | `exam.name`, `exam.date`, `exam.daysLeft` |
| Digest | `summary.count`, `summary.list` |
| AI | `ai.message`, `stepN.message` |

## Testing safely

Use **Test run** in the editor or `POST /api/workflows/{id}/test`. A preview run
evaluates conditions and renders every message, but sends nothing.
