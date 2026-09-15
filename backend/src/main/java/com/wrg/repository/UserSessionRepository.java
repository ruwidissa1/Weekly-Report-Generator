package com.wrg.repository;

import com.wrg.domain.UserSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<UserSession> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUserId(Long userId);
}
