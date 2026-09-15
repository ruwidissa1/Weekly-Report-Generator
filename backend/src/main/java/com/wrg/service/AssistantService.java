package com.wrg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrg.api.dto.AssistantDtos.ChatRequest;
import com.wrg.api.dto.AssistantDtos.ChatResponse;
import com.wrg.domain.AppUser;
import com.wrg.domain.ReportStatus;
import com.wrg.domain.WeeklyReport;
import com.wrg.repository.WeeklyReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssistantService {
    private final WeeklyReportRepository reports;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String openAiApiKey;
    private final String openAiModel;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String openRouterApiKey;
    private final String openRouterModel;

    public AssistantService(WeeklyReportRepository reports,
                            ObjectMapper objectMapper,
                            RestClient.Builder restClientBuilder,
                            @Value("${app.openai.api-key:}") String openAiApiKey,
                            @Value("${app.openai.model:gpt-5.6-terra}") String openAiModel,
                            @Value("${app.gemini.api-key:}") String geminiApiKey,
                            @Value("${app.gemini.model:gemini-3.7-flash}") String geminiModel,
                            @Value("${app.openrouter.api-key:}") String openRouterApiKey,
                            @Value("${app.openrouter.model:openrouter/free}") String openRouterModel) {
        this.reports = reports;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.openRouterApiKey = openRouterApiKey;
        this.openRouterModel = openRouterModel;
    }

    @Transactional(readOnly = true)
    public ChatResponse chat(AppUser user, ChatRequest request) {
        // Prefer the configured OpenRouter key, with Gemini/OpenAI kept as optional fallbacks.
        if (has(openRouterApiKey)) {
            return chatWithOpenRouter(user, request);
        }
        if (has(geminiApiKey)) {
            return chatWithGemini(user, request);
        }
        if (has(openAiApiKey)) {
            return chatWithOpenAi(user, request);
        }
        return new ChatResponse(
                "AI integration is not configured yet. Set OPENROUTER_API_KEY on the backend and restart Spring Boot.",
                "not-configured",
                false);
    }

    private ChatResponse chatWithOpenAi(AppUser user, ChatRequest request) {
        String input = promptInput(user, request);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("instructions", instructions());
        payload.put("input", input);
        payload.put("max_output_tokens", 700);

        try {
            String json = restClient.post()
                    .uri("https://api.openai.com/v1/responses")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return new ChatResponse(extractOpenAiAnswer(json), "openai/" + openAiModel, true);
        } catch (RestClientException ex) {
            return new ChatResponse("OpenAI request failed: " + ex.getMessage(), "openai/" + openAiModel, true);
        }
    }

    private ChatResponse chatWithGemini(AppUser user, ChatRequest request) {
        String encodedModel = UriUtils.encodePathSegment(normalizeGeminiModel(geminiModel), StandardCharsets.UTF_8);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + encodedModel + ":generateContent";
        Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instructions()))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", promptInput(user, request))))),
                "generationConfig", Map.of("maxOutputTokens", 700)
        );

        try {
            String json = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", geminiApiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return new ChatResponse(extractGeminiAnswer(json), "gemini/" + geminiModel, true);
        } catch (RestClientException ex) {
            return new ChatResponse("Gemini request failed: " + ex.getMessage(), "gemini/" + geminiModel, true);
        }
    }

    private ChatResponse chatWithOpenRouter(AppUser user, ChatRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openRouterModel);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", instructions()),
                Map.of("role", "user", "content", promptInput(user, request))
        ));
        payload.put("max_tokens", 700);

        try {
            String json = restClient.post()
                    .uri("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer " + openRouterApiKey)
                    .header("HTTP-Referer", "http://localhost:5173")
                    .header("X-OpenRouter-Title", "Weekly Report Generator")
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return new ChatResponse(extractChatCompletionAnswer(json), "openrouter/" + openRouterModel, true);
        } catch (RestClientException ex) {
            return new ChatResponse("OpenRouter request failed: " + ex.getMessage(), "openrouter/" + openRouterModel, true);
        }
    }

    private String promptInput(AppUser user, ChatRequest request) {
        return """
                Manager: %s

                Recent team report context:
                %s

                Recent chat:
                %s

                Manager question:
                %s
                """.formatted(user.getName(), reportContext(), historyText(request), request.message());
    }

    private String instructions() {
        return """
                You are the Weekly Report Generator AI assistant for managers.
                Answer only from the supplied report context. If the context does not contain enough information, say so.
                Keep answers concise, operational, and useful for team planning.
                Do not expose draft report content or claim access to data that was not supplied.
                """;
    }

    private boolean has(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeGeminiModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-3.7-flash";
        }
        return model.startsWith("models/") ? model.substring("models/".length()) : model;
    }

    private String reportContext() {
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(6);
        LocalDate end = LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6);
        // Only submitted/reviewed reports are sent to external AI providers.
        List<WeeklyReport> recentReports = reports.findByWeekStartGreaterThanEqualAndWeekEndLessThanEqual(start, end)
                .stream()
                .filter(report -> report.getStatus() != ReportStatus.DRAFT)
                .limit(30)
                .toList();

        if (recentReports.isEmpty()) {
            return "No submitted reports are available.";
        }

        StringBuilder builder = new StringBuilder();
        for (WeeklyReport report : recentReports) {
            builder.append("- ")
                    .append(report.getUser().getName())
                    .append(" | ")
                    .append(report.getProject().getName())
                    .append(" | ")
                    .append(report.getWeekStart())
                    .append(" to ")
                    .append(report.getWeekEnd())
                    .append(" | status ")
                    .append(report.getStatus())
                    .append(" | tasks ")
                    .append(report.getTasksCompleted().stream().map(task -> task.getName() + " [" + task.getStatus() + "]").toList())
                    .append(" | blockers ")
                    .append(report.getBlockers().stream().map(blocker -> blocker.getDescription() + (blocker.isKeyIssue() ? " (key)" : "")).toList())
                    .append(" | achievements ")
                    .append(report.getAchievements().stream().map(achievement -> achievement.getDescription() + (achievement.isKeyAchievement() ? " (key)" : "")).toList())
                    .append(" | hours ")
                    .append(report.getHours().stream().map(hours -> hours.getTaskType() + "=" + hours.getHours()).toList());
            if (report.getLatestReviewComment() != null) {
                builder.append(" | latest review comment ").append(report.getLatestReviewComment());
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String historyText(ChatRequest request) {
        if (request.history() == null || request.history().isEmpty()) {
            return "No previous chat turns.";
        }
        return request.history().stream()
                .filter(message -> message.content() != null && !message.content().isBlank())
                .limit(8)
                .map(message -> message.role() + ": " + message.content())
                .toList()
                .toString();
    }

    private String extractOpenAiAnswer(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.hasNonNull("output_text")) {
                return root.path("output_text").asText();
            }
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if (content.hasNonNull("text")) {
                        return content.path("text").asText();
                    }
                }
            }
        } catch (Exception ignored) {
            return "The assistant returned an unreadable response.";
        }
        return "The assistant did not return any text.";
    }

    private String extractGeminiAnswer(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode candidate : root.path("candidates")) {
                for (JsonNode part : candidate.path("content").path("parts")) {
                    if (part.hasNonNull("text")) {
                        return part.path("text").asText();
                    }
                }
            }
        } catch (Exception ignored) {
            return "Gemini returned an unreadable response.";
        }
        return "Gemini did not return any text.";
    }

    private String extractChatCompletionAnswer(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && !content.isNull()) {
                return content.asText();
            }
        } catch (Exception ignored) {
            return "The chat completion provider returned an unreadable response.";
        }
        return "The chat completion provider did not return any text.";
    }
}
