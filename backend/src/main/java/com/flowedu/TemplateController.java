package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Phase 7: ready-made one-click templates. */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final WorkflowService service;

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put(String.valueOf(kv[i]), kv[i + 1]);
        return map;
    }

    private static Map<String, Object> rule(String field, String op, String value) {
        return m("field", field, "op", op, "value", value);
    }

    private static Map<String, Object> step(String type, Map<String, Object> config) {
        return m("type", type, "config", config);
    }

    private static final List<Map<String, Object>> TEMPLATES = List.of(
        m("key", "low-attendance",
          "name", "Low Attendance Alert",
          "description", "When attendance is uploaded, find students under 75%, let AI write a friendly message, email the student and Telegram the teacher.",
          "badge", "Main demo",
          "trigger", m("type", "EVENT", "event", "ATTENDANCE_UPLOADED", "source", "ATTENDANCE"),
          "conditions", m("logic", "AND", "rules", List.of(rule("attendance.percent", "<", "75"))),
          "audience", m(),
          "steps", List.of(
              step("AI", m("task", "personalize", "tone", "friendly but firm",
                      "prompt", "Student {{student.name|there}} has {{attendance.percent}}% attendance in {{course.name}}. The required minimum is 75%. Ask them to attend upcoming classes.",
                      "fallback", "Hi {{student.name|there}}, your attendance in {{course.name}} is {{attendance.percent}}%, below the required 75%. Please attend the upcoming classes.")),
              step("EMAIL", m("recipient", "STUDENT",
                      "subject", "Low attendance alert: {{course.name}}",
                      "body", "{{ai.message}}")),
              step("TELEGRAM", m("recipient", "TEACHER",
                      "text", "{{student.name}} ({{student.rollNo}}) has {{attendance.percent}}% attendance in {{course.name}}.")))),

        m("key", "assignment-reminder",
          "name", "Assignment Reminder",
          "description", "Two days before the deadline, remind students who have not submitted yet.",
          "badge", "Date-based",
          "trigger", m("type", "DATE", "dateSource", "ASSIGNMENT_DEADLINE", "source", "ASSIGNMENT", "daysBefore", 2),
          "conditions", m("logic", "AND", "rules", List.of(rule("assignment.submitted", "==", "false"))),
          "audience", m(),
          "steps", List.of(
              step("EMAIL", m("recipient", "STUDENT",
                      "subject", "Reminder: {{assignment.title}} is due in {{assignment.daysLeft}} days",
                      "body", "Hi {{student.name|there}},\n\n{{assignment.title}} for {{course.name}} is due on {{assignment.deadline}} and we have not received your submission yet.\n\nPlease submit on time.")))),

        m("key", "exam-countdown",
          "name", "Exam Countdown",
          "description", "Three days before an exam, message every student of the course.",
          "badge", "Date-based",
          "trigger", m("type", "DATE", "dateSource", "EXAM_DATE", "source", "EXAM", "daysBefore", 3),
          "conditions", m("logic", "AND", "rules", List.of()),
          "audience", m(),
          "steps", List.of(
              step("EMAIL", m("recipient", "STUDENT",
                      "subject", "{{exam.name}} in {{exam.daysLeft}} days",
                      "body", "Hi {{student.name|there}},\n\n{{exam.name}} ({{course.name}}) is on {{exam.date}}, {{exam.daysLeft}} days from now. Good luck with your preparation!")),
              step("TELEGRAM", m("recipient", "STUDENT",
                      "text", "{{exam.name}} is in {{exam.daysLeft}} days ({{exam.date}}). Good luck, {{student.name|there}}!")))),

        m("key", "weekly-teacher-summary",
          "name", "Weekly Teacher Summary",
          "description", "Every Monday 8:00, email each teacher the list of students below 75% in their course.",
          "badge", "Schedule",
          "trigger", m("type", "SCHEDULE", "cron", "0 0 8 * * MON", "source", "ATTENDANCE"),
          "conditions", m("logic", "AND", "rules", List.of(rule("attendance.percent", "<", "75"))),
          "audience", m("digest", true),
          "steps", List.of(
              step("EMAIL", m("recipient", "TEACHER",
                      "subject", "Weekly summary: {{summary.count}} students below 75% in {{course.name}}",
                      "body", "Students under 75% attendance in {{course.name}}:\n\n{{summary.list}}")))));

    @GetMapping
    public List<Map<String, Object>> list() { return TEMPLATES; }

    /** Creates the workflow (disabled) so it can be test-run and edited before going live. */
    @SuppressWarnings("unchecked")
    @PostMapping("/{key}/use")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Map<String, Object> use(@PathVariable String key, Authentication auth) {
        Map<String, Object> t = TEMPLATES.stream().filter(x -> key.equals(x.get("key"))).findFirst().orElseThrow();
        var req = new WorkflowService.Req(
                (String) t.get("name"), (String) t.get("description"), false,
                (Map<String, Object>) t.get("trigger"), (Map<String, Object>) t.get("conditions"),
                (Map<String, Object>) t.get("audience"), (List<Map<String, Object>>) t.get("steps"));
        return service.dto(service.create(req, auth.getName()));
    }
}
