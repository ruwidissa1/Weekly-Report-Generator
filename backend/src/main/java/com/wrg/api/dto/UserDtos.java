package com.wrg.api.dto;

import com.wrg.domain.Role;

import java.time.Instant;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String name,
            String email,
            Role role,
            boolean active,
            Instant createdAt
    ) {
    }

    public record UpdateUserRequest(
            Role role,
            Boolean active
    ) {
    }

    public record MemberProfileResponse(
            UserResponse user,
            long reportsSubmitted,
            long reportsApproved,
            long reportsNeedingCorrection,
            Object recentReports
    ) {
    }
}
