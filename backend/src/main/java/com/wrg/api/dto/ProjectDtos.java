package com.wrg.api.dto;

import jakarta.validation.constraints.NotBlank;

public final class ProjectDtos {
    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotBlank String name,
            String description,
            Boolean active
    ) {
    }

    public record ProjectResponse(
            Long id,
            String name,
            String description,
            boolean active
    ) {
    }
}
