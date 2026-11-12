package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Phase 5 and 8: execution logs and the dashboard. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InsightsController {
    private final Repos.WorkflowRepo workflows;
    private final Repos.RunRepo runs;
    private final Repos.StepLogRepo stepLogs;
    private final Repos.StudentRepo students;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("workflows", workflows.count());
        m.put("activeWorkflows", workflows.findByEnabledTrue().size());
        m.put("messagesSent", stepLogs.countByStatusInAndStepTypeIn(
                List.of("SUCCESS", "SIMULATED"), List.of("EMAIL", "TELEGRAM", "WEBHOOK")));
        m.put("failures", stepLogs.countByStatus("FAILED"));
        m.put("activeStudents", students.count());
        m.put("recentRuns", runs.findTop10ByOrderByStartedAtDesc());

        // --- Chart data ---
        List<Domain.WorkflowRun> allRuns = runs.findTop100ByOrderByStartedAtDesc();
        List<Domain.StepLog> allSteps = stepLogs.findAll();

        // 1. Runs per day (last 7 days)
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MMM d");
        List<Map<String, Object>> runsPerDay = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = d.plusDays(1).atStartOfDay();
            long count = allRuns.stream()
                    .filter(r -> r.getStartedAt() != null && !r.getStartedAt().isBefore(start) && r.getStartedAt().isBefore(end))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", d.format(dayFmt));
            point.put("value", count);
            runsPerDay.add(point);
        }
        m.put("runsPerDay", runsPerDay);

        // 2. Message breakdown by channel type
        Map<String, Long> msgBreakdown = new LinkedHashMap<>();
        for (String type : List.of("EMAIL", "TELEGRAM", "WEBHOOK")) {
            msgBreakdown.put(type, stepLogs.countByStatusInAndStepTypeIn(
                    List.of("SUCCESS", "SIMULATED"), List.of(type)));
        }
        m.put("messagesByType", msgBreakdown);

        // 3. Run status distribution
        Map<String, Long> statusDist = allRuns.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStatus() != null ? r.getStatus() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.counting()));
        m.put("statusDistribution", statusDist);

        // 4. Step success vs failure over last 7 days
        List<Map<String, Object>> stepsByDay = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = d.plusDays(1).atStartOfDay();
            long successes = allSteps.stream()
                    .filter(s -> s.getLoggedAt() != null && !s.getLoggedAt().isBefore(start) && s.getLoggedAt().isBefore(end)
                            && ("SUCCESS".equals(s.getStatus()) || "SIMULATED".equals(s.getStatus())))
                    .count();
            long failures = allSteps.stream()
                    .filter(s -> s.getLoggedAt() != null && !s.getLoggedAt().isBefore(start) && s.getLoggedAt().isBefore(end)
                            && "FAILED".equals(s.getStatus()))
                    .count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", d.format(dayFmt));
            point.put("success", successes);
            point.put("failed", failures);
            stepsByDay.add(point);
        }
        m.put("stepsByDay", stepsByDay);

        return m;
    }

    @GetMapping("/runs")
    public List<Domain.WorkflowRun> listRuns() {
        return runs.findTop100ByOrderByStartedAtDesc();
    }

    /** One run with every step: status, attempts, input and output. */
    @GetMapping("/runs/{id}")
    public Map<String, Object> run(@PathVariable Long id) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("run", runs.findById(id).orElseThrow());
        m.put("steps", stepLogs.findByRunIdOrderByIdAsc(id));
        return m;
    }
}
