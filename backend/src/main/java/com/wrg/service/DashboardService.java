package com.wrg.service;

import com.wrg.api.dto.DashboardDtos.DashboardResponse;
import com.wrg.domain.AppUser;
import com.wrg.domain.ReportStatus;
import com.wrg.domain.Role;
import com.wrg.domain.TaskStatus;
import com.wrg.domain.WeeklyReport;
import com.wrg.repository.AppUserRepository;
import com.wrg.repository.ReviewCommentRepository;
import com.wrg.repository.WeeklyReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final WeeklyReportRepository reports;
    private final AppUserRepository users;
    private final ReviewCommentRepository reviewComments;

    public DashboardService(WeeklyReportRepository reports, AppUserRepository users, ReviewCommentRepository reviewComments) {
        this.reports = reports;
        this.users = users;
        this.reviewComments = reviewComments;
    }

    @Transactional(readOnly = true)
    public DashboardResponse overview(LocalDate weekStart) {
        LocalDate start = weekStart == null ? LocalDate.now().with(DayOfWeek.MONDAY) : weekStart;
        LocalDate end = start.plusDays(6);
        List<WeeklyReport> window = reports.findByWeekStartGreaterThanEqualAndWeekEndLessThanEqual(start.minusWeeks(5), end);
        List<WeeklyReport> thisWeek = window.stream().filter(r -> !r.getWeekStart().isBefore(start) && !r.getWeekEnd().isAfter(end)).toList();

        // Dashboard insights are manager side, so draft report content and counts are intentionally excluded
        List<WeeklyReport> visibleThisWeek = thisWeek.stream().filter(r -> r.getStatus() != ReportStatus.DRAFT).toList();
        List<WeeklyReport> visibleWindow = window.stream().filter(r -> r.getStatus() != ReportStatus.DRAFT).toList();
        var activeMembers = users.findByRoleAndActiveTrue(Role.TEAM_MEMBER);
        long memberCount = activeMembers.size();
        long submitted = visibleThisWeek.size();
        long notSubmitted = Math.max(0, memberCount - submitted);
        boolean selectedWeekIsLate = LocalDate.now().isAfter(end);
        long pending = selectedWeekIsLate ? 0 : notSubmitted;
        long late = selectedWeekIsLate ? notSubmitted : 0;
        double compliance = memberCount == 0 ? 0 : Math.round((submitted * 10000.0 / memberCount)) / 100.0;
        long corrections = visibleThisWeek.stream().filter(r -> r.getStatus() == ReportStatus.NEEDS_CORRECTION).count();
        long blockers = visibleThisWeek.stream()
                .filter(r -> r.getStatus() != ReportStatus.APPROVED)
                .mapToLong(r -> r.getBlockers().size())
                .sum();

        return new DashboardResponse(
                submitted,
                submitted,
                pending,
                late,
                compliance,
                corrections,
                blockers,
                tasksTrend(visibleWindow),
                statusByMember(activeMembers, thisWeek, selectedWeekIsLate),
                workloadByProject(visibleThisWeek),
                timeByTaskType(visibleThisWeek),
                reports.findTop10ByStatusNotOrderByUpdatedAtDesc(ReportStatus.DRAFT).stream().map(ReportMapper::toListItem).toList(),
                recentReviewActions(),
                sectionByMember(visibleThisWeek, true),
                sectionByMember(visibleThisWeek, false));
    }

    private List<Map<String, Object>> tasksTrend(List<WeeklyReport> source) {
        return source.stream()
                .collect(Collectors.groupingBy(WeeklyReport::getWeekStart, TreeMap::new, Collectors.summingInt(r ->
                        (int) r.getTasksCompleted().stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count())))
                .entrySet().stream()
                .map(entry -> map("week", entry.getKey().toString(), "tasks", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> statusByMember(List<AppUser> activeMembers, List<WeeklyReport> source, boolean selectedWeekIsLate) {
        Map<Long, WeeklyReport> reportsByMember = source.stream()
                .filter(report -> report.getStatus() != ReportStatus.DRAFT)
                .collect(Collectors.toMap(report -> report.getUser().getId(), report -> report, (first, second) -> first));

        // Missing visible reports become pending or late depending on whether the selected week has passed
        return activeMembers.stream()
                .sorted(Comparator.comparing(AppUser::getName))
                .map(member -> {
                    WeeklyReport report = reportsByMember.get(member.getId());
                    if (report == null) {
                        return map("member", member.getName(), "status", selectedWeekIsLate ? "LATE" : "PENDING", "reportId", null);
                    }
                    return map("member", member.getName(), "status", report.getStatus().name(), "reportId", report.getId());
                })
                .toList();
    }

    private List<Map<String, Object>> workloadByProject(List<WeeklyReport> source) {
        return source.stream()
                .collect(Collectors.groupingBy(r -> r.getProject().getName(), Collectors.summingInt(r -> r.getTasksCompleted().size())))
                .entrySet().stream()
                .map(entry -> map("project", entry.getKey(), "tasks", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> timeByTaskType(List<WeeklyReport> source) {
        Map<String, Double> totals = new LinkedHashMap<>();
        source.forEach(report -> report.getHours().forEach(hours ->
                totals.merge(hours.getTaskType(), hours.getHours(), Double::sum)));
        return totals.entrySet().stream()
                .map(entry -> map("type", entry.getKey(), "hours", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> recentReviewActions() {
        return reviewComments.findTop10ByOrderByCreatedAtDesc().stream()
                .map(comment -> map(
                        "reportId", comment.getReport().getId(),
                        "member", comment.getReport().getUser().getName(),
                        "project", comment.getReport().getProject().getName(),
                        "weekStart", comment.getReport().getWeekStart().toString(),
                        "reviewer", comment.getReviewer().getName(),
                        "action", comment.getResultingStatus().name(),
                        "versionNumber", comment.getVersionNumber(),
                        "comment", comment.getComment(),
                        "createdAt", comment.getCreatedAt()))
                .toList();
    }

    private List<Map<String, Object>> sectionByMember(List<WeeklyReport> source, boolean blockers) {
        return source.stream()
                .map(report -> map(
                        "member", report.getUser().getName(),
                        "project", report.getProject().getName(),
                        "items", blockers
                                ? report.getBlockers().stream().map(b -> b.getDescription() + (b.isKeyIssue() ? " (key)" : "")).toList()
                                : report.getAchievements().stream().map(a -> a.getDescription() + (a.isKeyAchievement() ? " (key)" : "")).toList()))
                .toList();
    }

    private Map<String, Object> map(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }
}
