package com.wrg.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "review_comments")
public class ReviewComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WeeklyReport report;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus resultingStatus;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(length = 3000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WeeklyReport getReport() {
        return report;
    }

    public void setReport(WeeklyReport report) {
        this.report = report;
    }

    public AppUser getReviewer() {
        return reviewer;
    }

    public void setReviewer(AppUser reviewer) {
        this.reviewer = reviewer;
    }

    public ReportStatus getResultingStatus() {
        return resultingStatus;
    }

    public void setResultingStatus(ReportStatus resultingStatus) {
        this.resultingStatus = resultingStatus;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
