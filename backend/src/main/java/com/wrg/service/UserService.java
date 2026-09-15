package com.wrg.service;

import com.wrg.api.dto.UserDtos.MemberProfileResponse;
import com.wrg.api.dto.UserDtos.UpdateUserRequest;
import com.wrg.api.dto.UserDtos.UserResponse;
import com.wrg.domain.AppUser;
import com.wrg.domain.ReportStatus;
import com.wrg.domain.Role;
import com.wrg.repository.AppUserRepository;
import com.wrg.repository.ReviewCommentRepository;
import com.wrg.repository.UserSessionRepository;
import com.wrg.repository.WeeklyReportRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    private final AppUserRepository users;
    private final WeeklyReportRepository reports;
    private final ReviewCommentRepository reviewComments;
    private final UserSessionRepository sessions;

    public UserService(AppUserRepository users,
                       WeeklyReportRepository reports,
                       ReviewCommentRepository reviewComments,
                       UserSessionRepository sessions) {
        this.users = users;
        this.reports = reports;
        this.reviewComments = reviewComments;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll().stream().map(UserService::toResponse).toList();
    }

    @Transactional
    public UserResponse update(Long id, AppUser currentUser, UpdateUserRequest request) {
        AppUser user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (isSelfAdminLockChange(user, currentUser, request)) {
            throw new AccessDeniedException("Admins cannot change their own role or deactivate themselves");
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        AppUser user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        // Protect account access by preventing the acting admin from removing their own login
        if (Objects.equals(user.getId(), currentUser.getId())) {
            throw new AccessDeniedException("Admins cannot delete themselves");
        }
        // At least one admin must remain .
        if (user.getRole() == Role.ADMIN && users.findByRoleAndActiveTrue(Role.ADMIN).size() <= 1) {
            throw new AccessDeniedException("At least one active admin is required");
        }
        // Review comments are audit history, so reviewers with past comments are retained
        if (reviewComments.existsByReviewerId(user.getId())) {
            throw new IllegalArgumentException("Users with review history cannot be deleted");
        }

        // A user's own reports can be removed with the account because they are not shared ownership records
        reports.deleteAll(reports.findByUserId(user.getId()));
        sessions.deleteByUserId(user.getId());
        user.getProjects().clear();
        users.delete(user);
    }

    private boolean isSelfAdminLockChange(AppUser user, AppUser currentUser, UpdateUserRequest request) {
        if (!Objects.equals(user.getId(), currentUser.getId()) || currentUser.getRole() != Role.ADMIN) {
            return false;
        }
        return request.active() != null && !request.active()
                || request.role() != null && request.role() != Role.ADMIN;
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse profile(Long id) {
        AppUser user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        var recent = reports.findByUserIdOrderByWeekStartDesc(id, PageRequest.of(0, 10))
                .map(ReportMapper::toListItem)
                .toList();
        long submittedOrReviewed = recent.stream().filter(item -> item.status() != ReportStatus.DRAFT).count();
        long approved = recent.stream().filter(item -> item.status() == ReportStatus.APPROVED).count();
        long correction = recent.stream().filter(item -> item.status() == ReportStatus.NEEDS_CORRECTION).count();
        return new MemberProfileResponse(toResponse(user), submittedOrReviewed, approved, correction, recent);
    }

    public static UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
