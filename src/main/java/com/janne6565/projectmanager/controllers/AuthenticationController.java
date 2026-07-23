package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.dto.AuthStatusResponse;
import com.janne6565.projectmanager.dto.LoginRequest;
import com.janne6565.projectmanager.dto.SessionResponse;
import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.services.auth.AuthService;
import com.janne6565.projectmanager.services.auth.RefreshCookieFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Login and token refresh return the access token in the body and set the
 * rotated {@code pm_refresh} cookie; logout clears it. Every login/logout response also purges the
 * stale legacy {@code JWT-TOKEN} cookie left over from the previous cookie-based scheme.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@RequestBody LoginRequest request) {
        return session(authService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/token")
    public ResponseEntity<SessionResponse> token(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false)
                    String refreshToken) {
        return session(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.logoutCookie().toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expireLegacy().toString())
                .build();
    }

    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated =
                authentication != null
                        && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getPrincipal());

        String username = null;
        if (isAuthenticated && authentication.getPrincipal() instanceof AppUser user) {
            username = user.getUsername();
        }

        return ResponseEntity.ok(
                AuthStatusResponse.builder().authenticated(isAuthenticated).username(username).build());
    }

    private ResponseEntity<SessionResponse> session(AuthService.AuthenticatedSession session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, session.refreshCookie().toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expireLegacy().toString())
                .body(session.body());
    }
}
