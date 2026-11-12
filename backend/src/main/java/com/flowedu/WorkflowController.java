package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 3 and 5: manage, run and test workflows. */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class WorkflowController {
    private final Repos.WorkflowRepo repo;
    private final Repos.RunRepo runs;
    private final Repos.StepLogRepo stepLogs;
    private final WorkflowService service;
    private final WorkflowEngine engine;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(service::dto).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return service.dto(repo.findById(id).orElseThrow());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody WorkflowService.Req r, Authentication auth) {
        return service.dto(service.create(r, auth.getName()));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody WorkflowService.Req r, Authentication auth) {
        return service.dto(service.save(repo.findById(id).orElseThrow(), r, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Enable / disable. */
    @PostMapping("/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id) {
        var wf = repo.findById(id).orElseThrow();
        wf.setEnabled(!Boolean.TRUE.equals(wf.getEnabled()));
        return service.dto(repo.save(wf));
    }

    @PostMapping("/{id}/clone")
    public Map<String, Object> clone(@PathVariable Long id, Authentication auth) {
        return service.dto(service.copy(repo.findById(id).orElseThrow(), auth.getName()));
    }

    /** Run now for real (ignores the trigger timing). Runs in the background. */
    @PostMapping("/{id}/run")
    public Domain.WorkflowRun run(@PathVariable Long id) {
        return engine.start(repo.findById(id).orElseThrow(), "MANUAL", WorkflowEngine.Mode.LIVE, null, false);
    }

    /**
     * Test run: send=false renders a preview and sends nothing;
     * send=true sends only to the logged-in user (email) and to the test Telegram chat.
     */
    @PostMapping("/{id}/test")
    public Map<String, Object> test(@PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean send,
                                    Authentication auth) {
        var wf = repo.findById(id).orElseThrow();
        var mode = send ? WorkflowEngine.Mode.SEND_TO_ME : WorkflowEngine.Mode.PREVIEW;
        var run = engine.start(wf, "TEST", mode, auth.getName(), true);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("run", run);
        out.put("steps", stepLogs.findByRunIdOrderByIdAsc(run.getId()));
        return out;
    }
}
