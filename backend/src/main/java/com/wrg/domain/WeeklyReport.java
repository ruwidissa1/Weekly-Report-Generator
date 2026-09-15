package com.wrg.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "week_start"})
})
public class WeeklyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.DRAFT;

    @ElementCollection
    @CollectionTable(name = "report_planned_tasks", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "task", length = 1000)
    private List<String> tasksPlannedNextWeek = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<ReportTask> tasksCompleted = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<ReportBlocker> blockers = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<ReportAchievement> achievements = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<HoursBreakdown> hours = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ReviewComment> reviewComments = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    private List<ReportVersion> versions = new ArrayList<>();

    @Column(length = 3000)
    private String notes;

    private String latestReviewComment;

    private Integer currentVersion = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();
    private Instant submittedAt;
    private Instant approvedAt;

    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(LocalDate weekEnd) {
        this.weekEnd = weekEnd;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public List<String> getTasksPlannedNextWeek() {
        return tasksPlannedNextWeek;
    }

    public void setTasksPlannedNextWeek(List<String> tasksPlannedNextWeek) {
        this.tasksPlannedNextWeek = tasksPlannedNextWeek;
    }

    public List<ReportTask> getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(List<ReportTask> tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public List<ReportBlocker> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<ReportBlocker> blockers) {
        this.blockers = blockers;
    }

    public List<ReportAchievement> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<ReportAchievement> achievements) {
        this.achievements = achievements;
    }

    public List<HoursBreakdown> getHours() {
        return hours;
    }

    public void setHours(List<HoursBreakdown> hours) {
        this.hours = hours;
    }

    public List<ReviewComment> getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(List<ReviewComment> reviewComments) {
        this.reviewComments = reviewComments;
    }

    public List<ReportVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<ReportVersion> versions) {
        this.versions = versions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLatestReviewComment() {
        return latestReviewComment;
    }

    public void setLatestReviewComment(String latestReviewComment) {
        this.latestReviewComment = latestReviewComment;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }
}
