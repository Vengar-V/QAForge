package com.qaforge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaforge.dto.AiGeneratorDto;
import com.qaforge.entity.FileRisk;
import com.qaforge.entity.Project;
import com.qaforge.entity.TestScenario;
import com.qaforge.repository.FileRiskRepository;
import com.qaforge.repository.TestScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorService {

    private final FileRiskRepository fileRiskRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final ObjectMapper objectMapper;

    // Groq використовує OpenAI-сумісний API
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    // ── Основний метод генерації ───────────────────────────────────────────────
    public AiGeneratorDto.GenerateResponse generate(Project project, AiGeneratorDto.GenerateRequest req) {
        List<FileRisk> fileRisks = resolveFileRisks(project, req.getFileRiskIds());
        if (fileRisks.isEmpty()) {
            throw new IllegalArgumentException("No file risks found for the given selection");
        }

        String prompt = buildPrompt(project, fileRisks, req);
        String rawJson = callGroq(req.getOpenaiApiKey(), prompt);
        List<ParsedScenario> parsed = parseScenarios(rawJson);
        List<TestScenario> saved = saveScenarios(parsed, project, fileRisks);

        List<AiGeneratorDto.ScenarioResponse> responses = saved.stream()
                .map(AiGeneratorDto.ScenarioResponse::from)
                .collect(Collectors.toList());

        return AiGeneratorDto.GenerateResponse.builder()
                .total(responses.size())
                .scenarios(responses)
                .build();
    }

    // ── Отримати збережені сценарії ────────────────────────────────────────────
    public List<AiGeneratorDto.ScenarioResponse> getScenarios(Project project) {
        return testScenarioRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream()
                .map(AiGeneratorDto.ScenarioResponse::from)
                .collect(Collectors.toList());
    }

    // ── Resolve FileRisk ───────────────────────────────────────────────────────
    private List<FileRisk> resolveFileRisks(Project project, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return fileRiskRepository.findByProjectOrderByRiskScoreDesc(project).stream()
                    .filter(fr -> fr.getRiskLevel() != FileRisk.RiskLevel.LOW)
                    .limit(10)
                    .collect(Collectors.toList());
        }
        return fileRiskRepository.findAllById(ids).stream()
                .filter(fr -> fr.getProject().getId().equals(project.getId()))
                .collect(Collectors.toList());
    }

    // ── Prompt ────────────────────────────────────────────────────────────────
    private String buildPrompt(Project project, List<FileRisk> fileRisks, AiGeneratorDto.GenerateRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert QA engineer. Generate test scenarios for the following project.\n\n");
        sb.append("Project: ").append(project.getName()).append("\n");
        if (project.getLanguage() != null) sb.append("Language: ").append(project.getLanguage()).append("\n");
        if (project.getRepoUrl() != null) sb.append("Repository: ").append(project.getRepoUrl()).append("\n");
        sb.append("\nHigh-risk files that need test coverage:\n");

        for (FileRisk fr : fileRisks) {
            sb.append(String.format("  - %s [Risk: %s, Bug count: %d, Change count: %d, Commits: %d]\n",
                    fr.getFilePath(), fr.getRiskLevel(), fr.getBugCount(),
                    fr.getChangeCount(), fr.getCommitCount()));
        }

        List<String> types = (req.getTestTypes() != null && !req.getTestTypes().isEmpty())
                ? req.getTestTypes()
                : List.of("UNIT", "INTEGRATION", "EDGE_CASE");

        sb.append("\nGenerate exactly ").append(req.getScenarioCount())
                .append(" test scenarios covering these test types: ")
                .append(String.join(", ", types)).append(".\n");

        sb.append("""

IMPORTANT: Respond ONLY with a valid JSON array. No markdown, no code fences, no explanation.
Start your response with [ and end with ]. Nothing before or after.

Each element must follow exactly this structure:
[
  {
    "title": "Short descriptive test title",
    "description": "What this test verifies and why it matters",
    "filePath": "exact/path/from/above.ext",
    "testType": "UNIT",
    "riskLevel": "HIGH",
    "steps": ["Step 1", "Step 2", "Step 3"],
    "expectedResult": "What should happen when test passes",
    "tags": ["tag1", "tag2"]
  }
]

Valid testType values: UNIT, INTEGRATION, EDGE_CASE, NEGATIVE, REGRESSION, SECURITY
Valid riskLevel values: HIGH, MEDIUM, LOW
""");
        return sb.toString();
    }

    // ── Виклик Groq API (OpenAI-сумісний формат) ──────────────────────────────
    private String callGroq(String apiKey, String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", GROQ_MODEL);
        body.put("temperature", 0.7);
        body.put("max_tokens", 8192);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are an expert QA engineer. Always respond with valid JSON only, no markdown."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(GROQ_URL, entity, JsonNode.class);
            JsonNode root = response.getBody();
            if (root == null) throw new RuntimeException("Empty response from Groq");

            // OpenAI-compatible format: choices[0].message.content
            String text = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

            log.debug("Groq response length: {} chars", text.length());
            return text;

        } catch (HttpClientErrorException e) {
            log.error("Groq API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(parseGroqError(e), e);
        } catch (Exception e) {
            log.error("Groq call failed", e);
            throw new RuntimeException("Groq connection error: " + e.getMessage(), e);
        }
    }

    // ── Парсинг помилки Groq ──────────────────────────────────────────────────
    private String parseGroqError(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        try {
            JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
            String message = root.path("error").path("message").asText("Unknown error");
            return switch (status) {
                case 401 -> "Invalid Groq API key. Get a free key at console.groq.com";
                case 429 -> "Groq rate limit hit. Please wait a moment and try again.";
                case 400 -> "Bad request to Groq: " + message;
                default  -> "Groq error (" + status + "): " + message;
            };
        } catch (Exception ex) {
            return "Groq error " + status + ": " + e.getMessage();
        }
    }

    // ── Парсинг JSON відповіді ─────────────────────────────────────────────────
    private List<ParsedScenario> parseScenarios(String rawText) {
        try {
            String clean = rawText.trim();
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```\\s*$", "").trim();
            }
            int start = clean.indexOf('[');
            int end = clean.lastIndexOf(']');
            if (start != -1 && end != -1 && end > start) {
                clean = clean.substring(start, end + 1);
            }
            return objectMapper.readValue(clean, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", rawText, e);
            throw new RuntimeException("Failed to parse AI response. Unexpected format.", e);
        }
    }

    // ── Збереження в БД ───────────────────────────────────────────────────────
    private List<TestScenario> saveScenarios(List<ParsedScenario> parsed, Project project, List<FileRisk> fileRisks) {
        Map<String, FileRisk> riskByPath = fileRisks.stream()
                .collect(Collectors.toMap(FileRisk::getFilePath, fr -> fr, (a, b) -> a));

        List<TestScenario> toSave = parsed.stream().map(p -> {
            FileRisk matched = riskByPath.get(p.filePath);
            TestScenario.TestType testType;
            try {
                testType = TestScenario.TestType.valueOf(p.testType);
            } catch (Exception e) {
                testType = TestScenario.TestType.UNIT;
            }
            return TestScenario.builder()
                    .project(project)
                    .fileRisk(matched)
                    .title(p.title)
                    .description(p.description)
                    .steps(p.steps != null ? p.steps : Collections.emptyList())
                    .expectedResult(p.expectedResult)
                    .testType(testType)
                    .status(TestScenario.TestStatus.DRAFT)
                    .source(TestScenario.TestSource.AI_GENERATED)
                    .tags(p.tags != null ? p.tags : Collections.emptyList())
                    .build();
        }).collect(Collectors.toList());

        return testScenarioRepository.saveAll(toSave);
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ParsedScenario {
        private String title;
        private String description;
        private String filePath;
        private String testType;
        private String riskLevel;
        private List<String> steps;
        private String expectedResult;
        private List<String> tags;
    }
}