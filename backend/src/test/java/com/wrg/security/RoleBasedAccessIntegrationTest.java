package com.wrg.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wrg.domain.AppUser;
import com.wrg.domain.ReportStatus;
import com.wrg.domain.Role;
import com.wrg.repository.AppUserRepository;
import com.wrg.repository.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleBasedAccessIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AppUserRepository users;

    @Autowired
    WeeklyReportRepository reports;

    @Test
    void teamMemberCannotUseManagerReportEndpointOrReadAnotherMembersReport() throws Exception {
        String memberToken = login("aria@wrg.local");
        String managerToken = login("manager@wrg.local");

        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        String managerReports = mockMvc.perform(get("/api/reports?size=50")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(managerReports).path("content");
        Long anotherMemberReportId = null;
        for (JsonNode report : content) {
            if (!"Aria Silva".equals(report.path("userName").asText())) {
                anotherMemberReportId = report.path("id").asLong();
                break;
            }
        }

        mockMvc.perform(get("/api/reports/" + anotherMemberReportId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanReadApprovedReportDetail() throws Exception {
        String managerToken = login("manager@wrg.local");
        String managerReports = mockMvc.perform(get("/api/reports?size=50")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(managerReports).path("content");
        Long approvedReportId = null;
        for (JsonNode report : content) {
            if ("APPROVED".equals(report.path("status").asText())) {
                approvedReportId = report.path("id").asLong();
                break;
            }
        }

        mockMvc.perform(get("/api/reports/" + approvedReportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void draftsAreOnlyVisibleToOwnerAndHiddenFromManagersAndAdmins() throws Exception {
        String managerToken = login("manager@wrg.local");
        String adminToken = login("admin@wrg.local");
        String chenToken = login("chen@wrg.local");
        Long draftId = reports.searchReports(null, null, ReportStatus.DRAFT, null, null, true, PageRequest.of(0, 1))
                .getContent()
                .get(0)
                .getId();

        String managerDraftReports = mockMvc.perform(get("/api/reports?status=DRAFT&size=50")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String adminDraftReports = mockMvc.perform(get("/api/reports?status=DRAFT&size=50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(0, objectMapper.readTree(managerDraftReports).path("content").size());
        assertEquals(0, objectMapper.readTree(adminDraftReports).path("content").size());

        mockMvc.perform(get("/api/reports/" + draftId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/" + draftId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        String draftDetail = mockMvc.perform(get("/api/reports/" + draftId)
                        .header("Authorization", "Bearer " + chenToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode report = objectMapper.readTree(draftDetail);
        assertEquals("DRAFT", report.path("status").asText());

        mockMvc.perform(put("/api/reports/" + report.path("id").asLong())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPayload(report, "Manager attempted to rewrite content")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardShowsComplianceAndInsightsWithoutDraftLeakage() throws Exception {
        String managerToken = login("manager@wrg.local");
        String dashboardJson = mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode dashboard = objectMapper.readTree(dashboardJson);
        assertTrue(dashboard.path("totalReportsThisWeek").asLong() > 0);
        assertTrue(dashboard.path("pendingThisWeek").asLong() + dashboard.path("lateThisWeek").asLong() > 0);
        assertTrue(dashboard.path("tasksTrend").isArray());
        assertTrue(dashboard.path("workloadByProject").isArray());
        assertTrue(dashboard.path("timeByTaskType").isArray());
        assertTrue(dashboard.path("recentReviewActions").isArray());

        for (JsonNode memberStatus : dashboard.path("statusByMember")) {
            assertTrue(!"DRAFT".equals(memberStatus.path("status").asText()));
            if ("PENDING".equals(memberStatus.path("status").asText()) || "LATE".equals(memberStatus.path("status").asText())) {
                assertTrue(!memberStatus.hasNonNull("reportId"));
            }
        }
    }

    @Test
    void selectedWeekTrackerShowsStatusesWithoutExposingDraftReports() throws Exception {
        String managerToken = login("manager@wrg.local");
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        String response = mockMvc.perform(get("/api/reports/team-week?weekStart=" + monday)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode statuses = objectMapper.readTree(response).path("memberStatuses");
        assertTrue(hasMemberStatus(statuses, "Aria Silva", "SUBMITTED", true));
        assertTrue(hasMemberStatus(statuses, "Ben Carter", "NEEDS_CORRECTION", true));
        assertTrue(hasMemberStatus(statuses, "Chen Liu", "DRAFT", false));
        assertTrue(hasMemberStatus(statuses, "Dina Patel", "NOT_STARTED", false));
    }

    @Test
    void onlyManagersCanDeleteCurrentlySubmittedReports() throws Exception {
        String managerToken = login("manager@wrg.local");
        String adminToken = login("admin@wrg.local");
        Long submittedReportId = firstReportIdWhere(managerToken, report ->
                "Dina Patel".equals(report.path("userName").asText()) && "SUBMITTED".equals(report.path("status").asText()));
        Long approvedReportId = firstReportIdWhere(managerToken, report -> "APPROVED".equals(report.path("status").asText()));
        assertNotNull(submittedReportId);
        assertNotNull(approvedReportId);

        mockMvc.perform(delete("/api/reports/" + submittedReportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reports/" + approvedReportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/reports/" + submittedReportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reports/" + submittedReportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminsCannotDemoteThemselvesButCanUpdateOtherAdmins() throws Exception {
        String adminToken = login("admin@wrg.local");
        Long adminId = firstUserIdWhere(adminToken, user -> "admin@wrg.local".equals(user.path("email").asText()));
        assertNotNull(adminId);

        mockMvc.perform(patch("/api/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isForbidden());

        AppUser secondAdmin = new AppUser();
        secondAdmin.setName("Second Admin");
        secondAdmin.setEmail("second-admin@wrg.local");
        secondAdmin.setPasswordHash("unused-test-password");
        secondAdmin.setRole(Role.ADMIN);
        users.save(secondAdmin);

        String response = mockMvc.perform(patch("/api/users/" + secondAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("MANAGER", objectMapper.readTree(response).path("role").asText());
    }

    @Test
    void adminsCanDeleteOtherUsersButNotThemselves() throws Exception {
        String adminToken = login("admin@wrg.local");
        Long adminId = firstUserIdWhere(adminToken, user -> "admin@wrg.local".equals(user.path("email").asText()));
        assertNotNull(adminId);

        mockMvc.perform(delete("/api/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        AppUser userToDelete = new AppUser();
        userToDelete.setName("Delete Candidate");
        userToDelete.setEmail("delete-candidate@wrg.local");
        userToDelete.setPasswordHash("unused-test-password");
        userToDelete.setRole(Role.TEAM_MEMBER);
        users.save(userToDelete);

        mockMvc.perform(delete("/api/users/" + userToDelete.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertTrue(users.findById(userToDelete.getId()).isEmpty());
    }

    @Test
    void teamMemberCannotEditAnotherMembersReport() throws Exception {
        String memberToken = login("aria@wrg.local");
        String managerToken = login("manager@wrg.local");
        Long anotherMemberReportId = firstReportIdWhere(managerToken, report -> !"Aria Silva".equals(report.path("userName").asText()));
        assertNotNull(anotherMemberReportId);

        String reportDetail = mockMvc.perform(get("/api/reports/" + anotherMemberReportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(put("/api/reports/" + anotherMemberReportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPayload(objectMapper.readTree(reportDetail), "Member attempted to rewrite another report")))
                .andExpect(status().isForbidden());
    }

    @Test
    void correctionCycleKeepsVersionHistoryAndCommentVersionLinks() throws Exception {
        String managerToken = login("manager@wrg.local");
        String ariaToken = login("aria@wrg.local");
        Long reportId = firstReportIdWhere(managerToken, report ->
                "Aria Silva".equals(report.path("userName").asText()) && "SUBMITTED".equals(report.path("status").asText()));
        assertNotNull(reportId);

        JsonNode original = getReport(reportId, managerToken);
        String originalNotes = original.path("notes").asText();

        mockMvc.perform(post("/api/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add API test evidence.\"}"))
                .andExpect(status().isOk());

        JsonNode returnedForCorrection = getReport(reportId, managerToken);
        assertEquals("NEEDS_CORRECTION", returnedForCorrection.path("status").asText());
        assertTrue(hasCommentForVersion(returnedForCorrection, "Please add API test evidence.", 1));

        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + ariaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPayload(returnedForCorrection, "Corrected report content with API test evidence.")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .header("Authorization", "Bearer " + ariaToken))
                .andExpect(status().isOk());

        JsonNode resubmitted = getReport(reportId, managerToken);
        assertEquals("SUBMITTED", resubmitted.path("status").asText());
        assertEquals(2, resubmitted.path("currentVersion").asInt());
        assertTrue(hasCommentForVersion(resubmitted, "Please add API test evidence.", 1));

        JsonNode firstVersion = snapshotForVersion(resubmitted, 1);
        JsonNode secondVersion = snapshotForVersion(resubmitted, 2);
        assertEquals(originalNotes, firstVersion.path("notes").asText());
        assertEquals("Corrected report content with API test evidence.", secondVersion.path("notes").asText());
    }

    private String login(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123"}
                """.formatted(email);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private JsonNode getReport(Long id, String token) throws Exception {
        String response = mockMvc.perform(get("/api/reports/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private Long firstReportIdWhere(String token, ReportPredicate predicate) throws Exception {
        String managerReports = mockMvc.perform(get("/api/reports?size=50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode report : objectMapper.readTree(managerReports).path("content")) {
            if (predicate.matches(report)) {
                return report.path("id").asLong();
            }
        }
        return null;
    }

    private Long firstUserIdWhere(String token, UserPredicate predicate) throws Exception {
        String response = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode user : objectMapper.readTree(response)) {
            if (predicate.matches(user)) {
                return user.path("id").asLong();
            }
        }
        return null;
    }

    private String reportPayload(JsonNode report, String notes) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("weekStart", report.path("weekStart").asText());
        payload.put("weekEnd", report.path("weekEnd").asText());
        payload.put("projectId", report.path("projectId").asLong());
        payload.set("tasksCompleted", report.path("tasksCompleted"));
        payload.set("tasksPlannedNextWeek", report.path("tasksPlannedNextWeek"));
        payload.set("blockers", report.path("blockers"));
        payload.set("achievements", report.path("achievements"));
        payload.set("hours", report.path("hours"));
        payload.put("notes", notes);
        return objectMapper.writeValueAsString(payload);
    }

    private boolean hasCommentForVersion(JsonNode report, String comment, int version) {
        for (JsonNode reviewComment : report.path("reviewComments")) {
            if (comment.equals(reviewComment.path("comment").asText()) && reviewComment.path("versionNumber").asInt() == version) {
                return true;
            }
        }
        return false;
    }

    private JsonNode snapshotForVersion(JsonNode report, int versionNumber) throws Exception {
        for (JsonNode version : report.path("versions")) {
            if (version.path("versionNumber").asInt() == versionNumber) {
                return objectMapper.readTree(version.path("snapshotJson").asText());
            }
        }
        throw new AssertionError("Snapshot v" + versionNumber + " not found");
    }

    private boolean hasMemberStatus(JsonNode statuses, String member, String expectedStatus, boolean shouldHaveReportId) {
        for (JsonNode status : statuses) {
            if (member.equals(status.path("userName").asText()) && expectedStatus.equals(status.path("status").asText())) {
                return shouldHaveReportId == status.hasNonNull("reportId");
            }
        }
        return false;
    }

    private interface ReportPredicate {
        boolean matches(JsonNode report);
    }

    private interface UserPredicate {
        boolean matches(JsonNode user);
    }
}
