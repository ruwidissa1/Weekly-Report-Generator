package com.wrg.repository;

import com.wrg.domain.ReportStatus;
import com.wrg.domain.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    @EntityGraph(attributePaths = {"user", "project"})
    Page<WeeklyReport> findByUserIdOrderByWeekStartDesc(Long userId, Pageable pageable);

    List<WeeklyReport> findByUserId(Long userId);

    Optional<WeeklyReport> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    @EntityGraph(attributePaths = {"user", "project"})
    List<WeeklyReport> findByWeekStart(LocalDate weekStart);

    @EntityGraph(attributePaths = {"user", "project"})
    @Query("""
            select r from WeeklyReport r
            where (:userId is null or r.user.id = :userId)
              and (:projectId is null or r.project.id = :projectId)
              and (:status is null or r.status = :status)
              and (:from is null or r.weekStart >= :from)
              and (:to is null or r.weekEnd <= :to)
              and (:includeDrafts = true or r.status <> com.wrg.domain.ReportStatus.DRAFT)
            order by r.weekStart desc, r.user.name asc
            """)
    Page<WeeklyReport> searchReports(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("status") ReportStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("includeDrafts") boolean includeDrafts,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "project"})
    List<WeeklyReport> findTop10ByStatusNotOrderByUpdatedAtDesc(ReportStatus status);

    long countByStatusAndWeekStartLessThanEqualAndWeekEndGreaterThanEqual(ReportStatus status, LocalDate weekStart, LocalDate weekEnd);

    long countByWeekStartLessThanEqualAndWeekEndGreaterThanEqual(LocalDate weekStart, LocalDate weekEnd);

    List<WeeklyReport> findByWeekStartGreaterThanEqualAndWeekEndLessThanEqual(LocalDate from, LocalDate to);
}
