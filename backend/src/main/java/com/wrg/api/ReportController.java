package com.wrg.api;

import com.wrg.api.dto.ReportDtos.ReportListItem;
import com.wrg.api.dto.ReportDtos.ReportRequest;
import com.wrg.api.dto.ReportDtos.ReportResponse;
import com.wrg.api.dto.ReportDtos.ReviewRequest;
import com.wrg.api.dto.ReportDtos.TeamWeekResponse;
import com.wrg.api.dto.PageResponse;
import com.wrg.domain.ReportStatus;
import com.wrg.security.CurrentUser;
import com.wrg.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/mine")
    public PageResponse<ReportListItem> mine(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(reports.ownReports(CurrentUser.require(), PageRequest.of(page, size)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public PageResponse<ReportListItem> all(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) Long projectId,
                                            @RequestParam(required = false) ReportStatus status,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(reports.managerReports(userId, projectId, status, from, to, pageable));
    }

    @GetMapping("/team-week")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public TeamWeekResponse teamWeek(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                                     @RequestParam(required = false) Long userId,
                                     @RequestParam(required = false) Long projectId) {
        return reports.teamWeek(weekStart, userId, projectId);
    }

    @GetMapping("/{id}")
    public ReportResponse get(@PathVariable Long id) {
        return reports.get(id, CurrentUser.require());
    }

    @PostMapping
    public ReportResponse create(@Valid @RequestBody ReportRequest request) {
        return reports.create(CurrentUser.require(), request);
    }

    @PutMapping("/{id}")
    public ReportResponse update(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        return reports.update(id, CurrentUser.require(), request);
    }

    @PostMapping("/{id}/submit")
    public ReportResponse submit(@PathVariable Long id) {
        return reports.submit(id, CurrentUser.require());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ReportResponse approve(@PathVariable Long id) {
        return reports.approve(id, CurrentUser.require());
    }

    @PostMapping("/{id}/request-changes")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ReportResponse requestChanges(@PathVariable Long id, @RequestBody ReviewRequest request) {
        return reports.requestChanges(id, CurrentUser.require(), request.comment());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubmitted(@PathVariable Long id) {
        reports.deleteSubmitted(id, CurrentUser.require());
    }
}
