package com.janne6565.projectmanager.security;

import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.entities.Role;
import com.janne6565.projectmanager.repositories.AppUserRepository;
import com.janne6565.projectmanager.services.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer <jwt>} header, verifies it is an access token, loads the
 * (enabled) {@link AppUser} by its UUID subject and populates the {@link SecurityContextHolder}. The
 * user is granted {@code ROLE_<r>} for every role at-or-below its own, so an {@code OWNER} satisfies
 * {@code hasRole("ADMIN")} too. A missing or invalid token leaves the context anonymous.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        UUID userId;
        try {
            // Only access tokens authenticate a request; a refresh token here stays anonymous.
            userId = jwtService.parseUserId(token, JwtService.TokenType.ACCESS);
        } catch (RuntimeException ex) {
            // Invalid/expired token — stay anonymous; protected routes are then rejected.
            return;
        }
        userRepository
                .findById(userId)
                .filter(AppUser::isEnabled)
                .ifPresent(
                        user -> {
                            var authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user, null, authorities(user));
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
    }

    /** Every role at or below the user's role, so OWNER implies ADMIN implies USER. */
    private List<SimpleGrantedAuthority> authorities(AppUser user) {
        return Arrays.stream(Role.values())
                .filter(role -> user.getRole().isAtLeast(role))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
