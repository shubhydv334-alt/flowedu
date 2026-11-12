package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Phase 3: storing workflows as JSON and validating what the builder sends. */
@Service
@RequiredArgsConstructor
public class WorkflowService {

    public record Req(String name, String description, Boolean enabled,
                      Map<String, Object> trigger, Map<String, Object> conditions,
                      Map<String, Object> audience, List<Map<String, Object>> steps) {}

    private static final Set<String> STEP_TYPES = Set.of("AI", "EMAIL", "TELEGRAM", "WEBHOOK");

    private final Repos.WorkflowRepo repo;

    public void validate(Req r) {
        if (r.name() == null || r.name().isBlank()) throw new IllegalArgumentException("Workflow name is required");
        if (r.trigger() == null) throw new IllegalArgumentException("A trigger is required");
        String type = String.valueOf(r.trigger().get("type"));
        switch (type) {
            case "EVENT" -> {
                if (r.trigger().get("event") == null) throw new IllegalArgumentException("Choose an event");
            }
            case "SCHEDULE" -> {
                String cron = String.valueOf(r.trigger().get("cron"));
                if (!CronExpression.isValidExpression(cron)) {
                    throw new IllegalArgumentException("Invalid cron expression. Use 6 fields, e.g. 0 0 8 * * MON");
                }
            }
            case "DATE" -> {
                if (r.trigger().get("daysBefore") == null) throw new IllegalArgumentException("Set how many days before");
            }
            default -> throw new IllegalArgumentException("Unknown trigger type: " + type);
        }
        if (r.steps() == null || r.steps().isEmpty()) throw new IllegalArgumentException("Add at least one action");
        for (Map<String, Object> s : r.steps()) {
            if (!STEP_TYPES.contains(String.valueOf(s.get("type")).toUpperCase())) {
                throw new IllegalArgumentException("Unknown action type: " + s.get("type"));
            }
        }
    }

    public Domain.Workflow save(Domain.Workflow wf, Req r, String user) {
        validate(r);
        wf.setName(r.name().trim());
        wf.setDescription(r.description());
        wf.setEnabled(r.enabled() != null ? r.enabled() : Boolean.TRUE.equals(wf.getEnabled()));
        wf.setTriggerJson(WorkflowSupport.json(r.trigger()));
        wf.setConditionsJson(WorkflowSupport.json(r.conditions() == null ? Map.of() : r.conditions()));
        wf.setAudienceJson(WorkflowSupport.json(r.audience() == null ? Map.of() : r.audience()));
        wf.setStepsJson(WorkflowSupport.json(r.steps()));
        if (wf.getCreatedAt() == null) {
            wf.setCreatedAt(LocalDateTime.now());
            wf.setCreatedBy(user);
        }
        return repo.save(wf);
    }

    public Domain.Workflow create(Req r, String user) {
        return save(new Domain.Workflow(), r, user);
    }

    public Domain.Workflow copy(Domain.Workflow src, String user) {
        Domain.Workflow c = new Domain.Workflow();
        c.setName(src.getName() + " (copy)");
        c.setDescription(src.getDescription());
        c.setEnabled(false);
        c.setTriggerJson(src.getTriggerJson());
        c.setConditionsJson(src.getConditionsJson());
        c.setAudienceJson(src.getAudienceJson());
        c.setStepsJson(src.getStepsJson());
        c.setCreatedAt(LocalDateTime.now());
        c.setCreatedBy(user);
        return repo.save(c);
    }

    public Map<String, Object> dto(Domain.Workflow wf) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", wf.getId());
        m.put("name", wf.getName());
        m.put("description", wf.getDescription());
        m.put("enabled", Boolean.TRUE.equals(wf.getEnabled()));
        m.put("trigger", WorkflowSupport.readMap(wf.getTriggerJson()));
        m.put("conditions", WorkflowSupport.readMap(wf.getConditionsJson()));
        m.put("audience", WorkflowSupport.readMap(wf.getAudienceJson()));
        m.put("steps", WorkflowSupport.readList(wf.getStepsJson()));
        m.put("createdBy", wf.getCreatedBy());
        m.put("createdAt", wf.getCreatedAt());
        m.put("lastRunAt", wf.getLastRunAt());
        return m;
    }
}
