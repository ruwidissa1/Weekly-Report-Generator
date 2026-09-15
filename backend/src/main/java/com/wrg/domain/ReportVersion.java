package com.wrg.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "report_versions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"report_id", "version_number"})
})
public class ReportVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WeeklyReport report;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

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

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
