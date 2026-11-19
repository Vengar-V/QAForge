package com.qaforge.controller;

import com.qaforge.dto.AiGeneratorDto;
import com.qaforge.entity.Project;
import com.qaforge.entity.User;
import com.qaforge.repository.ProjectRepository;
import com.qaforge.repository.UserRepository;
import com.qaforge.service.AiGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorController {

    private final AiGeneratorService aiGeneratorService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /** GET /api/ai/ping — діагностика */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "controller", "AiGeneratorController"));
    }

    /** POST /api/ai/{projectId}/generate */
    @PostMapping("/{projectId}/generate")
    public ResponseEntity<?> generate(
            @PathVariable UUID projectId,
            @RequestBody AiGeneratorDto.GenerateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Generate request: project={}, user={}", projectId, userDetails.getUsername());

        if (req.getOpenaiApiKey() == null || req.getOpenaiApiKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "OpenAI API key is required"));
        }
        if (req.getScenarioCount() <= 0) req.setScenarioCount(8);

        try {
            User user = getUser(userDetails);
            Project project = getProject(projectId, user);
            req.setProjectId(projectId);
            AiGeneratorDto.GenerateResponse response = aiGeneratorService.generate(project, req);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Generation failed: {}", e.getMessage());
            // Повертаємо зрозуміле повідомлення на фронтенд
            return ResponseEntity.status(422).body(Map.of("message", e.getMessage()));
        }
    }

    /** GET /api/ai/{projectId}/scenarios */
    @GetMapping("/{projectId}/scenarios")
    public ResponseEntity<List<AiGeneratorDto.ScenarioResponse>> getScenarios(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        Project project = getProject(projectId, user);
        return ResponseEntity.ok(aiGeneratorService.getScenarios(project));
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