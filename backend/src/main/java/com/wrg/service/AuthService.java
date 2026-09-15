package com.wrg.service;

import com.wrg.api.dto.AuthDtos.AuthResponse;
import com.wrg.api.dto.AuthDtos.LoginRequest;
import com.wrg.api.dto.AuthDtos.RegisterRequest;
import com.wrg.api.dto.AuthDtos.UserSummary;
import com.wrg.domain.AppUser;
import com.wrg.domain.Role;
import com.wrg.domain.UserSession;
import com.wrg.repository.AppUserRepository;
import com.wrg.repository.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, UserSessionRepository sessions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        AppUser user = new AppUser();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.TEAM_MEMBER);
        users.save(user);
        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return issueToken(user);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            sessions.deleteByToken(authorizationHeader.substring(7));
        }
    }

    private AuthResponse issueToken(AppUser user) {
        UserSession session = new UserSession();
        session.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        sessions.save(session);
        return new AuthResponse(session.getToken(), summary(user));
    }

    public static UserSummary summary(AppUser user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }
}
