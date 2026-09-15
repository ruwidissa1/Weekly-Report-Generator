package com.wrg.api.dto;

import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record DashboardResponse(
            long totalReportsThisWeek,
            long submittedThisWeek,
            long pendingThisWeek,
            long lateThisWeek,
            double complianceRate,
            long needsCorrection,
            long openBlockers,
            List<Map<String, Object>> tasksTrend,
            List<Map<String, Object>> statusByMember,
            List<Map<String, Object>> workloadByProject,
            List<Map<String, Object>> timeByTaskType,
            List<?> recentActivity,
            List<Map<String, Object>> recentReviewActions,
            List<Map<String, Object>> blockersByMember,
            List<Map<String, Object>> achievementsByMember
    ) {
    }
}
