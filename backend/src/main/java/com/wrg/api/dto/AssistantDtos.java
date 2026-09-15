package com.wrg.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class AssistantDtos {
    private AssistantDtos() {
    }

    public record ChatMessage(String role, String content) {
    }

    public record ChatRequest(
            @NotBlank String message,
            List<ChatMessage> history
    ) {
    }

    public record ChatResponse(
            String answer,
            String model,
            boolean configured
    ) {
    }
}
