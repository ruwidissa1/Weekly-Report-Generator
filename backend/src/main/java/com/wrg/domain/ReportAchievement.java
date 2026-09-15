package com.wrg.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "report_achievements")
public class ReportAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WeeklyReport report;

    @Column(nullable = false, length = 1500)
    private String description;

    @Column(nullable = false)
    private boolean keyAchievement;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isKeyAchievement() {
        return keyAchievement;
    }

    public void setKeyAchievement(boolean keyAchievement) {
        this.keyAchievement = keyAchievement;
    }
}
