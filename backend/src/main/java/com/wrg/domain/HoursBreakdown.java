package com.wrg.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "hours_breakdown")
public class HoursBreakdown {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WeeklyReport report;

    @Column(nullable = false)
    private String taskType;

    @Column(nullable = false)
    private Double hours;

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

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Double getHours() {
        return hours;
    }

    public void setHours(Double hours) {
        this.hours = hours;
    }
}
