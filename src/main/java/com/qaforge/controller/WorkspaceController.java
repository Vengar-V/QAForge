package com.qaforge.controller;

import com.qaforge.dto.WorkspaceDto;
import com.qaforge.entity.*;
import com.qaforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
@Slf4j
public class WorkspaceController {

    private final TestScenarioRepository scenarioRepository;
    private final BugReportRepository bugReportRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // ── SCENARIOS ─────────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/scenarios")
    public ResponseEntity<List<WorkspaceDto.ScenarioResponse>> getScenarios(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails ud) {
        Project project = getProject(projectId, getUser(ud));
        return ResponseEntity.ok(
            scenarioRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream().map(WorkspaceDto.ScenarioResponse::from).collect(Collectors.toList())
        );
    }

    @PutMapping("/{projectId}/scenarios/{scenarioId}")
    public ResponseEntity<WorkspaceDto.ScenarioResponse> updateScenario(
            @PathVariable UUID projectId,
            @PathVariable UUID scenarioId,
            @RequestBody WorkspaceDto.ScenarioRequest req,
            @AuthenticationPrincipal UserDetails ud) {

        getProject(projectId, getUser(ud));
        TestScenario s = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        if (req.getTitle()          != null) s.setTitle(req.getTitle());
        if (req.getDescription()    != null) s.setDescription(req.getDescription());
        if (req.getSteps()          != null) s.setSteps(req.getSteps());
        if (req.getExpectedResult() != null) s.setExpectedResult(req.getExpectedResult());
        if (req.getTags()           != null) s.setTags(req.getTags());

        if (req.getStatus() != null) {
            try { s.setStatus(TestScenario.TestStatus.valueOf(req.getStatus())); }
            catch (Exception ignored) {}
        }
        if (req.getTestType() != null) {
            try { s.setTestType(TestScenario.TestType.valueOf(req.getTestType())); }
            catch (Exception ignored) {}
        }

        return ResponseEntity.ok(WorkspaceDto.ScenarioResponse.from(scenarioRepository.save(s)));
    }

    @DeleteMapping("/{projectId}/scenarios/{scenarioId}")
    public ResponseEntity<Void> deleteScenario(
            @PathVariable UUID projectId,
            @PathVariable UUID scenarioId,
            @AuthenticationPrincipal UserDetails ud) {
        getProject(projectId, getUser(ud));
        scenarioRepository.deleteById(scenarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/scenarios")
    public ResponseEntity<WorkspaceDto.ScenarioResponse> createScenario(
            @PathVariable UUID projectId,
            @RequestBody WorkspaceDto.ScenarioRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        Project project = getProject(projectId, getUser(ud));

        TestScenario.TestType type = TestScenario.TestType.UNIT;
        try { type = TestScenario.TestType.valueOf(req.getTestType()); } catch (Exception ignored) {}

        TestScenario s = TestScenario.builder()
                .project(project)
                .title(req.getTitle())
                .description(req.getDescription())
                .steps(req.getSteps() != null ? req.getSteps() : List.of())
                .expectedResult(req.getExpectedResult())
                .testType(type)
                .status(TestScenario.TestStatus.DRAFT)
                .source(TestScenario.TestSource.MANUAL)
                .tags(req.getTags() != null ? req.getTags() : List.of())
                .build();

        return ResponseEntity.ok(WorkspaceDto.ScenarioResponse.from(scenarioRepository.save(s)));
    }

    // ── BUG REPORTS ───────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/bugs")
    public ResponseEntity<List<WorkspaceDto.BugResponse>> getBugs(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails ud) {
        Project project = getProject(projectId, getUser(ud));
        return ResponseEntity.ok(
            bugReportRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream().map(WorkspaceDto.BugResponse::from).collect(Collectors.toList())
        );
    }

    @PostMapping("/{projectId}/bugs")
    public ResponseEntity<WorkspaceDto.BugResponse> createBug(
            @PathVariable UUID projectId,
            @RequestBody WorkspaceDto.BugRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        Project project = getProject(projectId, getUser(ud));
        User user = getUser(ud);

        BugReport.Severity severity = BugReport.Severity.MINOR;
        try { severity = BugReport.Severity.valueOf(req.getSeverity()); } catch (Exception ignored) {}

        BugReport b = BugReport.builder()
                .project(project)
                .reporter(user)
                .title(req.getTitle())
                .stepsToReproduce(req.getStepsToReproduce() != null ? req.getStepsToReproduce() : List.of())
                .expectedResult(req.getExpectedResult())
                .actualResult(req.getActualResult())
                .severity(severity)
                .status(BugReport.BugStatus.OPEN)
                .filePath(req.getFilePath())
                .attachments(req.getAttachments() != null ? req.getAttachments() : List.of())
                .tags(req.getTags() != null ? req.getTags() : List.of())
                .build();

        return ResponseEntity.ok(WorkspaceDto.BugResponse.from(bugReportRepository.save(b)));
    }

    @PutMapping("/{projectId}/bugs/{bugId}")
    public ResponseEntity<WorkspaceDto.BugResponse> updateBug(
            @PathVariable UUID projectId,
            @PathVariable UUID bugId,
            @RequestBody WorkspaceDto.BugRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        getProject(projectId, getUser(ud));
        BugReport b = bugReportRepository.findById(bugId)
                .orElseThrow(() -> new RuntimeException("Bug not found"));

        if (req.getTitle()           != null) b.setTitle(req.getTitle());
        if (req.getStepsToReproduce()!= null) b.setStepsToReproduce(req.getStepsToReproduce());
        if (req.getExpectedResult()  != null) b.setExpectedResult(req.getExpectedResult());
        if (req.getActualResult()    != null) b.setActualResult(req.getActualResult());
        if (req.getFilePath()        != null) b.setFilePath(req.getFilePath());
        if (req.getTags()            != null) b.setTags(req.getTags());
        if (req.getAttachments()     != null) b.setAttachments(req.getAttachments());

        if (req.getSeverity() != null) {
            try { b.setSeverity(BugReport.Severity.valueOf(req.getSeverity())); } catch (Exception ignored) {}
        }
        if (req.getStatus() != null) {
            try { b.setStatus(BugReport.BugStatus.valueOf(req.getStatus())); } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(WorkspaceDto.BugResponse.from(bugReportRepository.save(b)));
    }

    @DeleteMapping("/{projectId}/bugs/{bugId}")
    public ResponseEntity<Void> deleteBug(
            @PathVariable UUID projectId,
            @PathVariable UUID bugId,
            @AuthenticationPrincipal UserDetails ud) {
        getProject(projectId, getUser(ud));
        bugReportRepository.deleteById(bugId);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Project getProject(UUID id, User user) {
        return projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }
}
