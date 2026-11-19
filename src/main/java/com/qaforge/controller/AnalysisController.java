package com.qaforge.controller;

import com.qaforge.entity.CommitHistory;
import com.qaforge.entity.FileRisk;
import com.qaforge.entity.Project;
import com.qaforge.entity.User;
import com.qaforge.repository.CommitHistoryRepository;
import com.qaforge.repository.FileRiskRepository;
import com.qaforge.repository.ProjectRepository;
import com.qaforge.repository.UserRepository;
import com.qaforge.service.GitAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final GitAnalysisService gitAnalysisService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CommitHistoryRepository commitHistoryRepository;
    private final FileRiskRepository fileRiskRepository;

    @PostMapping("/{projectId}/analyse")
    public ResponseEntity<String> analyseProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Project project = getProject(projectId, user);
        gitAnalysisService.analyzeProject(project);
        return ResponseEntity.ok("Analysis started for: " + project.getName());
    }

    @GetMapping("/{projectId}/commits")
    public ResponseEntity<List<CommitHistory>> getCommits(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Project project = getProject(projectId, user);
        return ResponseEntity.ok(commitHistoryRepository.findByProject(project));
    }

    @GetMapping("/{projectId}/risks")
    public ResponseEntity<List<FileRisk>> getFileRisks(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Project project = getProject(projectId, user);
        return ResponseEntity.ok(fileRiskRepository.findByProjectOrderByRiskScoreDesc(project));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Project getProject(UUID projectId, User user) {
        return projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }
}