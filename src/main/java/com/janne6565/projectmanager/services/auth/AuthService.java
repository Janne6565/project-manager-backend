package com.janne6565.projectmanager.services.auth;

import com.janne6565.projectmanager.dto.AuthUserResponse;
import com.janne6565.projectmanager.dto.SessionResponse;
import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.exceptions.UnauthorizedException;
import com.janne6565.projectmanager.repositories.AppUserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies credentials and mints the access/refresh token pair. A successful login (password or
 * refresh) yields both the {@link SessionResponse} body (access token) and the rotated {@code
 * pm_refresh} cookie the controller sets on the response.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Transactional(readOnly = true)
    public AuthenticatedSession login(String username, String password) {
        AppUser user =
                userRepository
                        .findByUsername(username)
                        .filter(AppUser::isEnabled)
                        // OAuth-only users have no password hash — never feed null to the encoder,
                        // and fail with the same generic message as any other bad credential.
                        .filter(u -> u.getPasswordHash() != null)
                        .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                        // Same message whether the user is unknown, disabled, password-less, or the
                        // password is wrong — never reveal which.
                        .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        return buildSession(user);
    }

    /**
     * Exchanges a refresh token (from the {@code pm_refresh} cookie) for a fresh access token and a
     * rotated refresh cookie. Rejects a missing, malformed, expired, non-refresh, or disabled-user
     * token with {@link UnauthorizedException}.
     */
    @Transactional(readOnly = true)
    public AuthenticatedSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        UUID userId = jwtService.parseUserId(refreshToken, JwtService.TokenType.REFRESH);
        AppUser user =
                userRepository
                        .findById(userId)
                        .filter(AppUser::isEnabled)
                        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        return buildSession(user);
    }

    /** The clearing cookie for logout — the controller returns it with a 204. */
    public ResponseCookie logoutCookie() {
        return refreshCookieFactory.expire();
    }

    /** Mints the access token + refresh cookie for an already-authenticated user. */
    public AuthenticatedSession buildSession(AppUser user) {
        JwtService.IssuedToken access = jwtService.issueAccessToken(user);
        JwtService.IssuedToken refresh = jwtService.issueRefreshToken(user);
        ResponseCookie cookie = refreshCookieFactory.create(refresh.token());
        SessionResponse body =
                new SessionResponse(access.token(), access.expiresAt(), AuthUserResponse.from(user));
        return new AuthenticatedSession(body, cookie);
    }

    /** The login response body plus the refresh cookie to set on the HTTP response. */
    public record AuthenticatedSession(SessionResponse body, ResponseCookie refreshCookie) {}
}
