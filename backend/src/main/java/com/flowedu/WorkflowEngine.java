package com.flowedu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 4/5: the execution engine.
 *
 * start()  creates a run record and dispatches it (async for live runs, sync for test runs).
 * Worker   resolves the audience, evaluates conditions, runs each step in order with retry,
 *          passes data between steps through the context map and writes execution logs.
 */
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    public enum Mode { LIVE, PREVIEW, SEND_TO_ME }

    private final Repos.RunRepo runs;
    private final Worker worker;

    public Domain.WorkflowRun start(Domain.Workflow wf, String triggerType, Mode mode, String testEmail, boolean sync) {
        Domain.WorkflowRun run = runs.save(Domain.WorkflowRun.builder()
                .workflowId(wf.getId()).workflowName(wf.getName())
                .status("RUNNING").triggerType(triggerType)
                .startedAt(LocalDateTime.now())
                .affected(0).failures(0).testRun(mode != Mode.LIVE)
                .build());
        if (sync) {
            worker.execute(run.getId(), mode, testEmail);
            return runs.findById(run.getId()).orElse(run);
        }
        worker.executeAsync(run.getId(), mode, testEmail);
        return run;
    }

    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class Worker {
        private final Repos.WorkflowRepo workflows;
        private final Repos.RunRepo runs;
        private final Repos.StepLogRepo stepLogs;
        private final Repos.StudentRepo students;
        private final Repos.AttendanceRepo attendance;
        private final Repos.AssignmentRepo assignments;
        private final Repos.SubmissionRepo submissions;
        private final Repos.ExamRepo exams;
        private final List<Action> actionBeans;

        @Value("${flowedu.test-telegram-chat-id:}")
        private String testChat;

        @Async
        public void executeAsync(Long runId, Mode mode, String testEmail) {
            execute(runId, mode, testEmail);
        }

        @SuppressWarnings("unchecked")
        public void execute(Long runId, Mode mode, String testEmail) {
            Domain.WorkflowRun run = runs.findById(runId).orElseThrow();
            int affected = 0, failures = 0;
            try {
                Domain.Workflow wf = workflows.findById(run.getWorkflowId()).orElseThrow();
                Map<String, Object> trigger = WorkflowSupport.readMap(wf.getTriggerJson());
                Map<String, Object> conditions = WorkflowSupport.readMap(wf.getConditionsJson());
                Map<String, Object> audience = WorkflowSupport.readMap(wf.getAudienceJson());
                List<Map<String, Object>> steps = WorkflowSupport.readList(wf.getStepsJson());

                boolean manual = mode != Mode.LIVE || String.valueOf(run.getTriggerType()).startsWith("MANUAL");
                List<Map<String, Object>> units = buildUnits(resolveSource(trigger));

                // Date-based trigger: only units exactly X days before the date (ignored for manual/test runs)
                if ("DATE".equals(trigger.get("type")) && !manual) {
                    int days = toInt(trigger.get("daysBefore"), 1);
                    String key = "ASSIGNMENT".equals(resolveSource(trigger)) ? "assignment.daysLeft" : "exam.daysLeft";
                    units = units.stream()
                            .filter(u -> u.get(key) instanceof Number n && n.intValue() == days)
                            .collect(Collectors.toList());
                }
                units = units.stream()
                        .filter(u -> WorkflowSupport.audienceMatches(audience, (Domain.Student) u.get("_student")))
                        .filter(u -> WorkflowSupport.matches(conditions, u))
                        .collect(Collectors.toList());

                boolean digest = Boolean.TRUE.equals(audience.get("digest"));
                if (digest) units = digest(units);
                if (mode != Mode.LIVE) units = units.stream().limit(3).collect(Collectors.toList());

                Action.Options opts = new Action.Options(mode == Mode.PREVIEW, mode == Mode.SEND_TO_ME, testEmail, testChat);
                for (Map<String, Object> unit : units) {
                    Map<String, Object> ctx = new LinkedHashMap<>(unit);
                    String who = digest
                            ? "Digest: " + WorkflowSupport.str(ctx.get("course.name"))
                            : WorkflowSupport.str(ctx.get("student.name"));
                    boolean ok = true;
                    for (int i = 0; i < steps.size() && ok; i++) {
                        ok = runStep(runId, i, steps.get(i), ctx, who, opts, mode);
                    }
                    affected++;
                    if (!ok) failures++;
                }
                run.setStatus(failures == 0 ? "SUCCESS" : failures == affected ? "FAILED" : "PARTIAL");

                wf.setLastRunAt(LocalDateTime.now());
                workflows.save(wf);
            } catch (Exception e) {
                log.error("Workflow run {} crashed", runId, e);
                run.setStatus("FAILED");
                run.setError(String.valueOf(e.getMessage()));
            }
            run.setAffected(affected);
            run.setFailures(failures);
            run.setFinishedAt(LocalDateTime.now());
            runs.save(run);
        }

        // ---------------------------------------------------------------- one step, with retry
        private boolean runStep(Long runId, int index, Map<String, Object> step, Map<String, Object> ctx,
                                String who, Action.Options opts, Mode mode) {
            String type = String.valueOf(step.get("type")).toUpperCase();
            Action action = actionBeans.stream().filter(a -> a.type().equals(type)).findFirst().orElse(null);
            Object rawCfg = step.get("config");
            Map<String, Object> cfg = rawCfg == null ? new LinkedHashMap<>()
                    : (Map<String, Object>) WorkflowSupport.deepRender(rawCfg, ctx);

            Domain.StepLog log = Domain.StepLog.builder()
                    .runId(runId).stepIndex(index + 1).stepType(type).studentName(who)
                    .attempts(0).inputJson(WorkflowSupport.json(cfg)).loggedAt(LocalDateTime.now()).build();

            if (action == null) {
                log.setStatus("FAILED");
                log.setError("Unknown step type: " + type);
                stepLogs.save(log);
                return false;
            }

            int maxAttempts = mode == Mode.LIVE ? 3 : 1;
            Exception last = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                log.setAttempts(attempt);
                try {
                    Map<String, Object> out = action.run(cfg, ctx, opts);
                    String prefix = "step" + (index + 1) + ".";
                    out.forEach((k, v) -> ctx.put(prefix + k, v));
                    if ("AI".equals(type) && out.get("message") != null) ctx.put("ai.message", out.get("message"));
                    boolean message = !"AI".equals(type);
                    log.setStatus(message && opts.dryRun() ? "PREVIEW"
                            : message && Boolean.TRUE.equals(out.get("simulated")) ? "SIMULATED" : "SUCCESS");
                    log.setOutputJson(WorkflowSupport.json(out));
                    log.setError(null);
                    stepLogs.save(log);
                    return true;
                } catch (Exception e) {
                    last = e;
                    if (attempt < maxAttempts) {
                        try { Thread.sleep(400L << (attempt - 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }

            // Fallback text: an AI step that cannot answer uses its plain template instead.
            if ("AI".equals(type)) {
                String fallback = WorkflowSupport.str(cfg.get("fallback"));
                if (fallback.isBlank()) fallback = WorkflowSupport.str(cfg.get("prompt"));
                if (!fallback.isBlank()) {
                    ctx.put("ai.message", fallback);
                    ctx.put("step" + (index + 1) + ".message", fallback);
                    log.setStatus("FALLBACK");
                    log.setOutputJson(WorkflowSupport.json(Map.of("message", fallback)));
                    log.setError(String.valueOf(last == null ? "" : last.getMessage()));
                    stepLogs.save(log);
                    return true;
                }
            }
            log.setStatus("FAILED");
            log.setError(String.valueOf(last == null ? "failed" : last.getMessage()));
            stepLogs.save(log);
            return false;
        }

        // ------------------------------------------------------------ building the data units
        private String resolveSource(Map<String, Object> trigger) {
            Object s = trigger.get("source");
            if (s != null && !String.valueOf(s).isBlank()) return String.valueOf(s);
            if ("EXAM_DATE".equals(trigger.get("dateSource"))) return "EXAM";
            if ("ASSIGNMENT_DEADLINE".equals(trigger.get("dateSource"))
                    || "ASSIGNMENT_SUBMITTED".equals(trigger.get("event"))) return "ASSIGNMENT";
            return "ATTENDANCE";
        }

        private Map<String, Object> base(Domain.Student s, Domain.Course c) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("_student", s);
            m.put("student.name", s.getName());
            m.put("student.email", s.getEmail());
            m.put("student.rollNo", s.getRollNo());
            m.put("student.telegramChatId", s.getTelegramChatId());
            m.put("student.department", s.getDepartment() == null ? "" : s.getDepartment().getName());
            m.put("student.semester", s.getSemester());
            m.put("student.section", s.getSection());
            m.put("student.batch", s.getBatch());
            if (c != null) {
                m.put("_course", c);
                m.put("course.name", c.getName());
                m.put("course.code", c.getCode());
                m.put("course.teacher", c.getTeacherName());
                m.put("course.teacherEmail", c.getTeacherEmail());
                m.put("course.teacherTelegram", c.getTeacherTelegramChatId());
            }
            return m;
        }

        private List<Map<String, Object>> buildUnits(String source) {
            List<Map<String, Object>> out = new ArrayList<>();
            LocalDate today = LocalDate.now();
            List<Domain.Student> all = students.findAll();
            switch (source) {
                case "ATTENDANCE" -> {
                    // latest record per student and course
                    Map<String, Domain.AttendanceRecord> latest = new LinkedHashMap<>();
                    attendance.findAll().stream()
                            .sorted(Comparator.comparing(Domain.AttendanceRecord::getRecordDate,
                                            Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
                                    .thenComparing(Domain.AttendanceRecord::getId))
                            .forEach(a -> latest.put(a.getStudent().getId() + ":" + a.getCourse().getId(), a));
                    for (Domain.AttendanceRecord a : latest.values()) {
                        Map<String, Object> c = base(a.getStudent(), a.getCourse());
                        c.put("attendance.percent", a.getPercent());
                        out.add(c);
                    }
                }
                case "ASSIGNMENT" -> {
                    for (Domain.Assignment a : assignments.findAll()) {
                        Set<Long> done = submissions.findByAssignment(a).stream()
                                .map(x -> x.getStudent().getId()).collect(Collectors.toSet());
                        for (Domain.Student s : all) {
                            if (!WorkflowSupport.inCourse(s, a.getCourse())) continue;
                            Map<String, Object> c = base(s, a.getCourse());
                            c.put("assignment.title", a.getTitle());
                            c.put("assignment.deadline", a.getDeadline().toLocalDate().toString());
                            c.put("assignment.submitted", done.contains(s.getId()));
                            c.put("assignment.daysLeft", ChronoUnit.DAYS.between(today, a.getDeadline().toLocalDate()));
                            out.add(c);
                        }
                    }
                }
                case "EXAM" -> {
                    for (Domain.Exam e : exams.findAll()) {
                        for (Domain.Student s : all) {
                            if (!WorkflowSupport.inCourse(s, e.getCourse())) continue;
                            Map<String, Object> c = base(s, e.getCourse());
                            c.put("exam.name", e.getName());
                            c.put("exam.date", e.getExamDate().toString());
                            c.put("exam.daysLeft", ChronoUnit.DAYS.between(today, e.getExamDate()));
                            out.add(c);
                        }
                    }
                }
                default -> all.forEach(s -> out.add(base(s, null)));
            }
            return out;
        }

        /** Weekly teacher summary: collapse the matching units into one message per course. */
        private List<Map<String, Object>> digest(List<Map<String, Object>> units) {
            Map<Object, List<Map<String, Object>>> byCourse = new LinkedHashMap<>();
            for (Map<String, Object> u : units) {
                Domain.Course c = (Domain.Course) u.get("_course");
                byCourse.computeIfAbsent(c == null ? "none" : c.getId(), k -> new ArrayList<>()).add(u);
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (List<Map<String, Object>> group : byCourse.values()) {
                Map<String, Object> c = new LinkedHashMap<>(group.get(0));
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> u : group) {
                    sb.append("- ").append(WorkflowSupport.str(u.get("student.name")))
                      .append(" (").append(WorkflowSupport.str(u.get("student.rollNo"))).append(")");
                    if (u.get("attendance.percent") != null) {
                        sb.append(": ").append(WorkflowSupport.str(u.get("attendance.percent"))).append("%");
                    }
                    sb.append("\n");
                }
                c.put("summary.count", group.size());
                c.put("summary.list", sb.toString().trim());
                out.add(c);
            }
            return out;
        }

        private static int toInt(Object o, int dflt) {
            try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return dflt; }
        }
    }
}
