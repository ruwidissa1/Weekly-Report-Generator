package com.wrg.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "report_tasks")
public class ReportTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private WeeklyReport report;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority = TaskPriority.MEDIUM;

    private Integer plannedPercent;
    private Integer actualPercent;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.IN_PROGRESS;

    private Double timePlannedHours;
    private Double timeSpentHours;

    @Column(length = 1500)
    private String deliverable;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Integer getPlannedPercent() {
        return plannedPercent;
    }

    public void setPlannedPercent(Integer plannedPercent) {
        this.plannedPercent = plannedPercent;
    }

    public Integer getActualPercent() {
        return actualPercent;
    }

    public void setActualPercent(Integer actualPercent) {
        this.actualPercent = actualPercent;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Double getTimePlannedHours() {
        return timePlannedHours;
    }

    public void setTimePlannedHours(Double timePlannedHours) {
        this.timePlannedHours = timePlannedHours;
    }

    public Double getTimeSpentHours() {
        return timeSpentHours;
    }

    public void setTimeSpentHours(Double timeSpentHours) {
        this.timeSpentHours = timeSpentHours;
    }

    public String getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(String deliverable) {
        this.deliverable = deliverable;
    }
}
