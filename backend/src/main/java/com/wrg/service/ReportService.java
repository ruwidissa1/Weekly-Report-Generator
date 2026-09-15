package com.wrg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrg.api.dto.ReportDtos.ReportListItem;
import com.wrg.api.dto.ReportDtos.ReportRequest;
import com.wrg.api.dto.ReportDtos.ReportResponse;
import com.wrg.api.dto.ReportDtos.TeamMemberReportStatus;
import com.wrg.api.dto.ReportDtos.TeamWeekResponse;
import com.wrg.domain.*;
import com.wrg.repository.AppUserRepository;
import com.wrg.repository.ProjectRepository;
import com.wrg.repository.WeeklyReportRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportService {
    private final WeeklyReportRepository reports;
    private final ProjectRepository projects;
    private final AppUserRepository users;
    private final ObjectMapper objectMapper;

    public ReportService(WeeklyReportRepository reports, ProjectRepository projects, AppUserRepository users, ObjectMapper objectMapper) {
        this.reports = reports;
        this.projects = projects;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<ReportListItem> ownReports(AppUser user, Pageable pageable) {
        return reports.findByUserIdOrderByWeekStartDesc(user.getId(), pageable).map(ReportMapper::toListItem);
    }

    @Transactional(readOnly = true)
    public Page<ReportListItem> managerReports(Long userId, Long projectId, ReportStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        // Managers can search reviewed/submitted work, but draft rows stay private to the owner.
        return reports.searchReports(userId, projectId, status, from, to, false, pageable).map(ReportMapper::toListItem);
    }

    @Transactional(readOnly = true)
    public TeamWeekResponse teamWeek(LocalDate weekStart, Long userId, Long projectId) {
        LocalDate start = weekStart == null ? LocalDate.now().with(java.time.DayOfWeek.MONDAY) : weekStart;
        List<AppUser> members = users.findByRoleAndActiveTrue(Role.TEAM_MEMBER).stream()
                .filter(member -> userId == null || member.getId().equals(userId))
                .sorted(Comparator.comparing(AppUser::getName))
                .toList();
        List<WeeklyReport> weekReports = reports.findByWeekStart(start).stream()
                .filter(report -> userId == null || report.getUser().getId().equals(userId))
                .filter(report -> projectId == null || report.getStatus() == ReportStatus.DRAFT || report.getProject().getId().equals(projectId))
                .toList();
        Map<Long, WeeklyReport> reportByUser = new LinkedHashMap<>();
        for (WeeklyReport report : weekReports) {
            reportByUser.put(report.getUser().getId(), report);
        }
        List<TeamMemberReportStatus> statuses = members.stream()
                .map(member -> teamStatus(member, reportByUser.get(member.getId()), projectId))
                .filter(Objects::nonNull)
                .toList();
        List<WeeklyReport> visibleReports = weekReports.stream()
                .filter(report -> report.getStatus() != ReportStatus.DRAFT)
                .filter(report -> projectId == null || report.getProject().getId().equals(projectId))
                .toList();
        // Section comparisons only use visible reports
        return new TeamWeekResponse(
                start,
                start.plusDays(6),
                statuses,
                sectionByMember(visibleReports, true),
                sectionByMember(visibleReports, false));
    }

    @Transactional(readOnly = true)
    public ReportResponse get(Long id, AppUser currentUser) {
        WeeklyReport report = load(id);
        ensureCanView(report, currentUser);
        return ReportMapper.toResponse(report, true);
    }

    @Transactional
    public ReportResponse create(AppUser user, ReportRequest request) {
        reports.findByUserIdAndWeekStart(user.getId(), request.weekStart()).ifPresent(existing -> {
            throw new IllegalArgumentException("A report already exists for this week");
        });
        WeeklyReport report = new WeeklyReport();
        report.setUser(user);
        applyContent(report, request);
        return ReportMapper.toResponse(reports.save(report), true);
    }

    @Transactional
    public ReportResponse update(Long id, AppUser currentUser, ReportRequest request) {
        WeeklyReport report = load(id);
        ensureOwner(report, currentUser);
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new IllegalArgumentException("Only draft or needs-correction reports can be edited");
        }
        applyContent(report, request);
        return ReportMapper.toResponse(report, true);
    }

    @Transactional
    public ReportResponse submit(Long id, AppUser currentUser) {
        WeeklyReport report = load(id);
        ensureOwner(report, currentUser);
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new IllegalArgumentException("Only draft or needs-correction reports can be submitted");
        }
        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedAt(Instant.now());
        report.setApprovedAt(null);
        report.setLatestReviewComment(null);
        // Every submission becomes a preserved version for correction-cycle history.
        snapshotVersion(report);
        return ReportMapper.toResponse(report, true);
    }

    @Transactional
    public ReportResponse approve(Long id, AppUser reviewer) {
        WeeklyReport report = load(id);
        ensureManager(reviewer);
        ensureSubmitted(report);
        ReviewComment comment = reviewComment(report, reviewer, ReportStatus.APPROVED, "Approved");
        report.getReviewComments().add(comment);
        report.setStatus(ReportStatus.APPROVED);
        report.setApprovedAt(Instant.now());
        report.setLatestReviewComment("Approved");
        return ReportMapper.toResponse(report, true);
    }

    @Transactional
    public ReportResponse requestChanges(Long id, AppUser reviewer, String message) {
        WeeklyReport report = load(id);
        ensureManager(reviewer);
        ensureSubmitted(report);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A correction comment is required");
        }
        ReviewComment comment = reviewComment(report, reviewer, ReportStatus.NEEDS_CORRECTION, message);
        report.getReviewComments().add(comment);
        report.setStatus(ReportStatus.NEEDS_CORRECTION);
        report.setLatestReviewComment(message);
        return ReportMapper.toResponse(report, true);
    }

    @Transactional
    public void deleteSubmitted(Long id, AppUser currentUser) {
        // The assignment allows managers, not admins, to remove submitted reports from review.
        ensureManagerOnly(currentUser);
        WeeklyReport report = load(id);
        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only submitted reports can be deleted");
        }
        reports.delete(report);
    }

    private WeeklyReport load(Long id) {
        return reports.findById(id).orElseThrow(() -> new EntityNotFoundException("Report not found"));
    }

    private void applyContent(WeeklyReport report, ReportRequest request) {
        if (request.weekEnd().isBefore(request.weekStart())) {
            throw new IllegalArgumentException("Week end must be after week start");
        }
        Project project = projects.findById(request.projectId()).orElseThrow(() -> new EntityNotFoundException("Project not found"));
        report.setProject(project);
        report.setWeekStart(request.weekStart());
        report.setWeekEnd(request.weekEnd());
        report.setNotes(request.notes());
        report.setTasksPlannedNextWeek(cleanStrings(request.tasksPlannedNextWeek()));

        report.getTasksCompleted().clear();
        if (request.tasksCompleted() != null) {
            request.tasksCompleted().stream()
                    .filter(item -> item.name() != null && !item.name().isBlank())
                    .forEach(item -> {
                        ReportTask task = new ReportTask();
                        task.setReport(report);
                        task.setName(item.name());
                        task.setPriority(item.priority() == null ? TaskPriority.MEDIUM : item.priority());
                        task.setPlannedPercent(item.plannedPercent());
                        task.setActualPercent(item.actualPercent());
                        task.setStatus(item.status() == null ? TaskStatus.IN_PROGRESS : item.status());
                        task.setTimePlannedHours(item.timePlannedHours());
                        task.setTimeSpentHours(item.timeSpentHours());
                        task.setDeliverable(item.deliverable());
                        report.getTasksCompleted().add(task);
                    });
        }

        report.getBlockers().clear();
        if (request.blockers() != null) {
            request.blockers().stream()
                    .filter(item -> item.description() != null && !item.description().isBlank())
                    .forEach(item -> {
                        ReportBlocker blocker = new ReportBlocker();
                        blocker.setReport(report);
                        blocker.setDescription(item.description());
                        blocker.setKeyIssue(item.keyIssue());
                        report.getBlockers().add(blocker);
                    });
        }

        report.getAchievements().clear();
        if (request.achievements() != null) {
            request.achievements().stream()
                    .filter(item -> item.description() != null && !item.description().isBlank())
                    .forEach(item -> {
                        ReportAchievement achievement = new ReportAchievement();
                        achievement.setReport(report);
                        achievement.setDescription(item.description());
                        achievement.setKeyAchievement(item.keyAchievement());
                        report.getAchievements().add(achievement);
                    });
        }

        report.getHours().clear();
        if (request.hours() != null) {
            request.hours().stream()
                    .filter(item -> item.taskType() != null && !item.taskType().isBlank() && item.hours() != null)
                    .forEach(item -> {
                        HoursBreakdown hours = new HoursBreakdown();
                        hours.setReport(report);
                        hours.setTaskType(item.taskType());
                        hours.setHours(item.hours());
                        report.getHours().add(hours);
                    });
        }
    }

    private List<String> cleanStrings(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private void snapshotVersion(WeeklyReport report) {
        int nextVersion = report.getCurrentVersion() + 1;
        report.setCurrentVersion(nextVersion);
        ReportVersion version = new ReportVersion();
        version.setReport(report);
        version.setVersionNumber(nextVersion);
        version.setSubmittedAt(Instant.now());
        try {
            version.setSnapshotJson(objectMapper.writeValueAsString(ReportMapper.toResponse(report, false)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to snapshot report version", ex);
        }
        report.getVersions().add(version);
    }

    private ReviewComment reviewComment(WeeklyReport report, AppUser reviewer, ReportStatus status, String message) {
        ReviewComment comment = new ReviewComment();
        comment.setReport(report);
        comment.setReviewer(reviewer);
        comment.setResultingStatus(status);
        // Comments are tied to the submitted version that the manager actually reviewed.
        comment.setVersionNumber(report.getCurrentVersion());
        comment.setComment(message);
        return comment;
    }

    private TeamMemberReportStatus teamStatus(AppUser member, WeeklyReport report, Long projectId) {
        if (report == null) {
            return new TeamMemberReportStatus(member.getId(), member.getName(), "NOT_STARTED", null, null, null, null, false);
        }
        if (report.getStatus() == ReportStatus.DRAFT) {
            if (projectId != null && !report.getProject().getId().equals(projectId)) {
                return new TeamMemberReportStatus(member.getId(), member.getName(), "NOT_STARTED", null, null, null, null, false);
            }
            // Managers may track that a draft exists, but cannot open it or infer its project details.
            return new TeamMemberReportStatus(member.getId(), member.getName(), "DRAFT", null, null, null, null, false);
        }
        return new TeamMemberReportStatus(
                member.getId(),
                member.getName(),
                report.getStatus().name(),
                report.getId(),
                report.getProject().getName(),
                report.getCurrentVersion(),
                report.getLatestReviewComment(),
                true);
    }

    private List<Map<String, Object>> sectionByMember(List<WeeklyReport> source, boolean blockers) {
        return source.stream()
                .map(report -> map(
                        "member", report.getUser().getName(),
                        "project", report.getProject().getName(),
                        "reportId", report.getId(),
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

    private void ensureCanView(WeeklyReport report, AppUser user) {
        if (report.getUser().getId().equals(user.getId())) {
            return;
        }
        ensureManager(user);
        // Owner-only drafts are enforced here as a final guard even if a caller has a report id.
        if (report.getStatus() == ReportStatus.DRAFT) {
            throw new AccessDeniedException("Draft reports are only visible to the report owner");
        }
    }

    private void ensureOwner(WeeklyReport report, AppUser user) {
        if (!report.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Users can only edit their own reports");
        }
    }

    private void ensureManager(AppUser user) {
        if (user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Manager role required");
        }
    }

    private void ensureManagerOnly(AppUser user) {
        if (user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Manager role required");
        }
    }

    private void ensureSubmitted(WeeklyReport report) {
        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only submitted reports can be reviewed");
        }
    }
}
