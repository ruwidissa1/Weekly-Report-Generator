package com.wrg.service;

import com.wrg.api.dto.ReportDtos.*;
import com.wrg.domain.*;

import java.util.List;

public final class ReportMapper {
    private ReportMapper() {
    }

    public static ReportListItem toListItem(WeeklyReport report) {
        return new ReportListItem(
                report.getId(),
                report.getUser().getId(),
                report.getUser().getName(),
                report.getProject().getName(),
                report.getWeekStart(),
                report.getWeekEnd(),
                report.getStatus(),
                report.getCurrentVersion(),
                report.getLatestReviewComment(),
                report.getUpdatedAt());
    }

    public static ReportResponse toResponse(WeeklyReport report, boolean includeHistory) {
        return new ReportResponse(
                report.getId(),
                report.getUser().getId(),
                report.getUser().getName(),
                report.getProject().getId(),
                report.getProject().getName(),
                report.getWeekStart(),
                report.getWeekEnd(),
                report.getStatus(),
                report.getCurrentVersion(),
                report.getTasksCompleted().stream().map(ReportMapper::task).toList(),
                List.copyOf(report.getTasksPlannedNextWeek()),
                report.getBlockers().stream().map(ReportMapper::blocker).toList(),
                report.getAchievements().stream().map(ReportMapper::achievement).toList(),
                report.getHours().stream().map(ReportMapper::hours).toList(),
                report.getNotes(),
                report.getLatestReviewComment(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getSubmittedAt(),
                report.getApprovedAt(),
                includeHistory ? report.getReviewComments().stream().map(ReportMapper::comment).toList() : null,
                includeHistory ? report.getVersions().stream().map(ReportMapper::version).toList() : null);
    }

    private static TaskResponse task(ReportTask task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getPriority(),
                task.getPlannedPercent(),
                task.getActualPercent(),
                task.getStatus(),
                task.getTimePlannedHours(),
                task.getTimeSpentHours(),
                task.getDeliverable());
    }

    private static IssueResponse blocker(ReportBlocker blocker) {
        return new IssueResponse(blocker.getId(), blocker.getDescription(), blocker.isKeyIssue());
    }

    private static AchievementResponse achievement(ReportAchievement achievement) {
        return new AchievementResponse(achievement.getId(), achievement.getDescription(), achievement.isKeyAchievement());
    }

    private static HoursResponse hours(HoursBreakdown hours) {
        return new HoursResponse(hours.getId(), hours.getTaskType(), hours.getHours());
    }

    private static ReviewCommentResponse comment(ReviewComment comment) {
        return new ReviewCommentResponse(
                comment.getId(),
                comment.getReviewer().getName(),
                comment.getResultingStatus(),
                comment.getVersionNumber(),
                comment.getComment(),
                comment.getCreatedAt());
    }

    private static VersionResponse version(ReportVersion version) {
        return new VersionResponse(version.getId(), version.getVersionNumber(), version.getSubmittedAt(), version.getSnapshotJson());
    }
}
