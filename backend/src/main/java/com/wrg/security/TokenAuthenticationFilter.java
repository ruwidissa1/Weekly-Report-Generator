package com.wrg.security;

import com.wrg.domain.AppUser;
import com.wrg.domain.UserSession;
import com.wrg.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final UserSessionRepository sessions;

    public TokenAuthenticationFilter(UserSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            sessions.findByToken(token)
                    .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                    .map(UserSession::getUser)
                    .filter(AppUser::isActive)
                    .ifPresent(user -> {
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
