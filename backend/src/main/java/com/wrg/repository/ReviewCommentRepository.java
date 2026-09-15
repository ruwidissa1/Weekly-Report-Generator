package com.wrg.repository;

import com.wrg.domain.ReviewComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    @EntityGraph(attributePaths = {"reviewer", "report", "report.user", "report.project"})
    List<ReviewComment> findTop10ByOrderByCreatedAtDesc();

    boolean existsByReviewerId(Long reviewerId);
}
