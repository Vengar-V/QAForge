package com.qaforge.dto;

import com.qaforge.entity.TestScenario;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AiGeneratorDto {

    // ── REQUEST ─────────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateRequest {
        private UUID projectId;
        private List<UUID> fileRiskIds;          // конкретні файли для генерації
        private List<String> testTypes;          // UNIT, INTEGRATION, EDGE_CASE, etc.
        private int scenarioCount;               // бажана кількість сценаріїв
        private String contextSource;            // GIT_AND_CODE | CODE_ONLY | GIT_ONLY
        private String openaiApiKey;             // передається з фронтенду (не зберігається)
    }

    // ── RESPONSE (один сценарій) ─────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScenarioResponse {
        private UUID id;
        private String title;
        private String description;
        private List<String> steps;
        private String expectedResult;
        private String testType;
        private String riskLevel;
        private String filePath;
        private List<String> tags;
        private LocalDateTime createdAt;

        public static ScenarioResponse from(com.qaforge.entity.TestScenario s) {
            return ScenarioResponse.builder()
                    .id(s.getId())
                    .title(s.getTitle())
                    .description(s.getDescription())
                    .steps(s.getSteps())
                    .expectedResult(s.getExpectedResult())
                    .testType(s.getTestType().name())
                    .riskLevel(s.getFileRisk() != null ? s.getFileRisk().getRiskLevel().name() : null)
                    .filePath(s.getFileRisk() != null ? s.getFileRisk().getFilePath() : null)
                    .tags(s.getTags())
                    .createdAt(s.getCreatedAt())
                    .build();
        }
    }

    // ── RESPONSE (весь результат генерації) ──────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateResponse {
        private int total;
        private List<ScenarioResponse> scenarios;
    }
}