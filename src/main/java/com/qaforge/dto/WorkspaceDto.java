package com.qaforge.dto;

import com.qaforge.entity.BugReport;
import com.qaforge.entity.TestScenario;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WorkspaceDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScenarioRequest {
        private String title;
        private String description;
        private List<String> steps;
        private String expectedResult;
        private String testType;
        private String status;
        private List<String> tags;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ScenarioResponse {
        private UUID id;
        private String title;
        private String description;
        private List<String> steps;
        private String expectedResult;
        private String testType;
        private String status;
        private String source;
        private String riskLevel;
        private String filePath;
        private List<String> tags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ScenarioResponse from(TestScenario s) {
            return ScenarioResponse.builder()
                    .id(s.getId()).title(s.getTitle()).description(s.getDescription())
                    .steps(s.getSteps()).expectedResult(s.getExpectedResult())
                    .testType(s.getTestType() != null ? s.getTestType().name() : null)
                    .status(s.getStatus() != null ? s.getStatus().name() : null)
                    .source(s.getSource() != null ? s.getSource().name() : null)
                    .riskLevel(s.getFileRisk() != null ? s.getFileRisk().getRiskLevel().name() : null)
                    .filePath(s.getFileRisk() != null ? s.getFileRisk().getFilePath() : null)
                    .tags(s.getTags()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
                    .build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BugRequest {
        private String title;
        private List<String> stepsToReproduce;
        private String expectedResult;
        private String actualResult;
        private String severity;
        private String status;
        private String filePath;
        private List<String> tags;
        private List<String> attachments; // URLs or file links
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BugResponse {
        private UUID id;
        private String title;
        private List<String> stepsToReproduce;
        private String expectedResult;
        private String actualResult;
        private String severity;
        private String status;
        private String filePath;
        private List<String> tags;
        private List<String> attachments;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static BugResponse from(BugReport b) {
            return BugResponse.builder()
                    .id(b.getId()).title(b.getTitle())
                    .stepsToReproduce(b.getStepsToReproduce())
                    .expectedResult(b.getExpectedResult()).actualResult(b.getActualResult())
                    .severity(b.getSeverity() != null ? b.getSeverity().name() : null)
                    .status(b.getStatus() != null ? b.getStatus().name() : null)
                    .filePath(b.getFilePath()).tags(b.getTags())
                    .attachments(b.getAttachments())
                    .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt())
                    .build();
        }
    }
}
