package com.wrg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrg.domain.*;
import com.wrg.repository.AppUserRepository;
import com.wrg.repository.ProjectRepository;
import com.wrg.repository.WeeklyReportRepository;
import com.wrg.service.ReportMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final ProjectRepository projects;
    private final WeeklyReportRepository reports;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public DataSeeder(AppUserRepository users,
                      ProjectRepository projects,
                      WeeklyReportRepository reports,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper objectMapper,
                      @Value("${app.seed.enabled:true}") boolean enabled) {
        this.users = users;
        this.projects = projects;
        this.reports = reports;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!enabled) {
            return;
        }
        boolean emptyDatabase = users.count() == 0;
        ensureDefaultAdmin();
        if (!emptyDatabase) {
            return;
        }

        Project clientA = project("Client A", "Customer delivery and integrations");
        Project tooling = project("Internal Tooling", "Developer productivity and internal systems");
        Project rnd = project("R&D", "Research spikes and product discovery");
        projects.saveAll(List.of(clientA, tooling, rnd));

        AppUser manager = user("Maya Manager", "manager@wrg.local", Role.MANAGER);
        AppUser aria = user("Aria Silva", "aria@wrg.local", Role.TEAM_MEMBER);
        AppUser ben = user("Ben Carter", "ben@wrg.local", Role.TEAM_MEMBER);
        AppUser chen = user("Chen Liu", "chen@wrg.local", Role.TEAM_MEMBER);
        AppUser dina = user("Dina Patel", "dina@wrg.local", Role.TEAM_MEMBER);
        users.saveAll(List.of(manager, aria, ben, chen, dina));

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        reports.save(report(aria, clientA, monday.minusWeeks(2), ReportStatus.APPROVED, "Approved", 1));
        reports.save(report(ben, tooling, monday.minusWeeks(2), ReportStatus.APPROVED, "Approved", 1));
        reports.save(report(chen, rnd, monday.minusWeeks(1), ReportStatus.NEEDS_CORRECTION, "Add clearer deliverables for the research spike.", 1));
        reports.save(report(dina, clientA, monday.minusWeeks(1), ReportStatus.SUBMITTED, null, 1));
        reports.save(report(aria, tooling, monday, ReportStatus.SUBMITTED, null, 1));
        reports.save(report(ben, clientA, monday, ReportStatus.NEEDS_CORRECTION, "Please split meeting hours from development time.", 2));
        reports.save(report(chen, rnd, monday, ReportStatus.DRAFT, null, 0));
    }

    private void ensureDefaultAdmin() {
        if (!users.findByRoleAndActiveTrue(Role.ADMIN).isEmpty()) {
            return;
        }
        AppUser admin = users.findByEmailIgnoreCase("admin@wrg.local")
                .orElseGet(() -> user("Admin User", "admin@wrg.local", Role.ADMIN));
        admin.setName("Admin User");
        admin.setEmail("admin@wrg.local");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        users.save(admin);
    }

    private Project project(String name, String description) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        return project;
    }

    private AppUser user(String name, String email, Role role) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return user;
    }

    private WeeklyReport report(AppUser user, Project project, LocalDate start, ReportStatus status, String comment, int versions) throws Exception {
        WeeklyReport report = new WeeklyReport();
        report.setUser(user);
        report.setProject(project);
        report.setWeekStart(start);
        report.setWeekEnd(start.plusDays(6));
        report.setStatus(status);
        report.setSubmittedAt(status == ReportStatus.DRAFT ? null : Instant.now().minusSeconds(3600));
        report.setApprovedAt(status == ReportStatus.APPROVED ? Instant.now().minusSeconds(900) : null);
        report.setLatestReviewComment(comment);
        report.setNotes("Links: Jira board, deployment notes, and customer feedback summary.");
        report.setTasksPlannedNextWeek(List.of("Close carry-over QA tasks", "Prepare rollout notes", "Review risk log"));

        task(report, "Delivered weekly milestone for " + project.getName(), TaskPriority.HIGH, 100, 95, TaskStatus.COMPLETED, 16.0, 17.5, "Merged feature branch and release notes");
        task(report, "Coordinated review and QA follow-up", TaskPriority.MEDIUM, 80, 70, TaskStatus.IN_PROGRESS, 8.0, 7.0, "QA checklist and bug triage notes");
        blocker(report, "Waiting for stakeholder confirmation on scope", true);
        blocker(report, "One dependency required clarification", false);
        achievement(report, "Reduced open defects before handoff", true);
        achievement(report, "Improved documentation for onboarding", false);
        hours(report, "Development", 18.5);
        hours(report, "Testing", 7.0);
        hours(report, "Meetings", 4.0);
        hours(report, "Documentation", 3.5);

        report.setCurrentVersion(versions);
        for (int i = 1; i <= versions; i++) {
            ReportVersion version = new ReportVersion();
            version.setReport(report);
            version.setVersionNumber(i);
            version.setSnapshotJson(objectMapper.writeValueAsString(ReportMapper.toResponse(report, false)));
            version.setSubmittedAt(Instant.now().minusSeconds((long) (versions - i + 1) * 7200));
            report.getVersions().add(version);
        }
        if (comment != null) {
            ReviewComment review = new ReviewComment();
            review.setReport(report);
            review.setReviewer(users.findByEmailIgnoreCase("manager@wrg.local").orElseThrow());
            review.setResultingStatus(status);
            review.setVersionNumber(Math.max(1, versions));
            review.setComment(comment);
            report.getReviewComments().add(review);
        }
        return report;
    }

    private void task(WeeklyReport report, String name, TaskPriority priority, int planned, int actual, TaskStatus status, double plannedHours, double spentHours, String deliverable) {
        ReportTask task = new ReportTask();
        task.setReport(report);
        task.setName(name);
        task.setPriority(priority);
        task.setPlannedPercent(planned);
        task.setActualPercent(actual);
        task.setStatus(status);
        task.setTimePlannedHours(plannedHours);
        task.setTimeSpentHours(spentHours);
        task.setDeliverable(deliverable);
        report.getTasksCompleted().add(task);
    }

    private void blocker(WeeklyReport report, String text, boolean key) {
        ReportBlocker blocker = new ReportBlocker();
        blocker.setReport(report);
        blocker.setDescription(text);
        blocker.setKeyIssue(key);
        report.getBlockers().add(blocker);
    }

    private void achievement(WeeklyReport report, String text, boolean key) {
        ReportAchievement achievement = new ReportAchievement();
        achievement.setReport(report);
        achievement.setDescription(text);
        achievement.setKeyAchievement(key);
        report.getAchievements().add(achievement);
    }

    private void hours(WeeklyReport report, String type, double value) {
        HoursBreakdown hours = new HoursBreakdown();
        hours.setReport(report);
        hours.setTaskType(type);
        hours.setHours(value);
        report.getHours().add(hours);
    }
}
