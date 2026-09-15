package com.wrg.api.dto;

import com.wrg.domain.ReportStatus;
import com.wrg.domain.TaskPriority;
import com.wrg.domain.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record ReportRequest(
            @NotNull LocalDate weekStart,
            @NotNull LocalDate weekEnd,
            @NotNull Long projectId,
            @Valid List<TaskRequest> tasksCompleted,
            List<String> tasksPlannedNextWeek,
            @Valid List<IssueRequest> blockers,
            @Valid List<AchievementRequest> achievements,
            @Valid List<HoursRequest> hours,
            String notes
    ) {
    }

    public record TaskRequest(
            String name,
            TaskPriority priority,
            Integer plannedPercent,
            Integer actualPercent,
            TaskStatus status,
            Double timePlannedHours,
            Double timeSpentHours,
            String deliverable
    ) {
    }

    public record IssueRequest(String description, boolean keyIssue) {
    }

    public record AchievementRequest(String description, boolean keyAchievement) {
    }

    public record HoursRequest(String taskType, Double hours) {
    }

    public record ReviewRequest(String comment) {
    }

    public record ReportResponse(
            Long id,
            Long userId,
            String userName,
            Long projectId,
            String projectName,
            LocalDate weekStart,
            LocalDate weekEnd,
            ReportStatus status,
            Integer currentVersion,
            List<TaskResponse> tasksCompleted,
            List<String> tasksPlannedNextWeek,
            List<IssueResponse> blockers,
            List<AchievementResponse> achievements,
            List<HoursResponse> hours,
            String notes,
            String latestReviewComment,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant approvedAt,
            List<ReviewCommentResponse> reviewComments,
            List<VersionResponse> versions
    ) {
    }

    public record ReportListItem(
            Long id,
            Long userId,
            String userName,
            String projectName,
            LocalDate weekStart,
            LocalDate weekEnd,
            ReportStatus status,
            Integer currentVersion,
            String latestReviewComment,
            Instant updatedAt
    ) {
    }

    public record TaskResponse(
            Long id,
            String name,
            TaskPriority priority,
            Integer plannedPercent,
            Integer actualPercent,
            TaskStatus status,
            Double timePlannedHours,
            Double timeSpentHours,
            String deliverable
    ) {
    }

    public record IssueResponse(Long id, String description, boolean keyIssue) {
    }

    public record AchievementResponse(Long id, String description, boolean keyAchievement) {
    }

    public record HoursResponse(Long id, String taskType, Double hours) {
    }

    public record ReviewCommentResponse(
            Long id,
            String reviewerName,
            ReportStatus resultingStatus,
            Integer versionNumber,
            String comment,
            Instant createdAt
    ) {
    }

    public record VersionResponse(
            Long id,
            Integer versionNumber,
            Instant submittedAt,
            String snapshotJson
    ) {
    }

    public record TeamWeekResponse(
            LocalDate weekStart,
            LocalDate weekEnd,
            List<TeamMemberReportStatus> memberStatuses,
            List<Map<String, Object>> blockersByMember,
            List<Map<String, Object>> achievementsByMember
    ) {
    }

    public record TeamMemberReportStatus(
            Long userId,
            String userName,
            String status,
            Long reportId,
            String projectName,
            Integer currentVersion,
            String latestReviewComment,
            boolean canOpen
    ) {
    }
}
