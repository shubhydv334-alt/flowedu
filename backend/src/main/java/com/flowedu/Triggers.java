package com.flowedu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Phase 4: the three trigger types. Event, schedule (cron) and date-based. */
public final class Triggers {
    private Triggers() {}

    // ---- Domain events published by the import and assignment endpoints
    public record AttendanceUploaded(int rows) {}
    public record AssignmentSubmitted(Long assignmentId, Long studentId) {}

    // ---- Event triggers
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class EventTriggers {
        private final Repos.WorkflowRepo workflows;
        private final WorkflowEngine engine;

        @TransactionalEventListener(fallbackExecution = true)
        public void onAttendance(AttendanceUploaded e) { fire("ATTENDANCE_UPLOADED"); }

        @TransactionalEventListener(fallbackExecution = true)
        public void onSubmitted(AssignmentSubmitted e) { fire("ASSIGNMENT_SUBMITTED"); }

        private void fire(String event) {
            for (Domain.Workflow wf : workflows.findByEnabledTrue()) {
                var t = WorkflowSupport.readMap(wf.getTriggerJson());
                if ("EVENT".equals(t.get("type")) && event.equals(t.get("event"))) {
                    log.info("Event {} fires workflow '{}'", event, wf.getName());
                    engine.start(wf, "EVENT:" + event, WorkflowEngine.Mode.LIVE, null, false);
                }
            }
        }
    }

    // ---- Schedule (cron) and date-based triggers
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class TimeTriggers {
        private final Repos.WorkflowRepo workflows;
        private final WorkflowEngine engine;

        /** Every minute: start schedule workflows whose cron matches this minute. */
        @Scheduled(cron = "0 * * * * *")
        public void scheduleTick() {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            for (Domain.Workflow wf : workflows.findByEnabledTrue()) {
                var t = WorkflowSupport.readMap(wf.getTriggerJson());
                if (!"SCHEDULE".equals(t.get("type"))) continue;
                try {
                    CronExpression cron = CronExpression.parse(String.valueOf(t.get("cron")));
                    LocalDateTime next = cron.next(now.minusSeconds(1));
                    if (next != null && next.equals(now)) {
                        engine.start(wf, "SCHEDULE", WorkflowEngine.Mode.LIVE, null, false);
                    }
                } catch (Exception ex) {
                    log.warn("Bad cron on workflow {}: {}", wf.getId(), ex.getMessage());
                }
            }
        }

        /** Once a day: date-based workflows ("X days before ...") select their own matching rows. */
        @Scheduled(cron = "${flowedu.date-trigger-cron:0 0 9 * * *}")
        public void dateTick() {
            for (Domain.Workflow wf : workflows.findByEnabledTrue()) {
                var t = WorkflowSupport.readMap(wf.getTriggerJson());
                if ("DATE".equals(t.get("type"))) {
                    engine.start(wf, "DATE", WorkflowEngine.Mode.LIVE, null, false);
                }
            }
        }
    }
}
