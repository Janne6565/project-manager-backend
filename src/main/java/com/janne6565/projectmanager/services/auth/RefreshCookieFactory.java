package com.janne6565.projectmanager.services.auth;

import com.janne6565.projectmanager.configs.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the {@code pm_refresh} httpOnly refresh cookie. {@code SameSite=Lax} is required so the
 * cookie survives the top-level redirect back from the OAuth provider; the path is the full
 * browser-visible {@code /api/v1/auth} (the servlet context-path is {@code /api/v1}) so it is only
 * sent to the token/logout endpoints, and {@code Secure} follows {@code jwt.cookie.secure}.
 *
 * <p>Also produces an already-expired legacy {@code JWT-TOKEN} cookie (path {@code /}) so that stale
 * cookies left over from the previous cookie-based scheme get purged from browsers on login/logout.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "pm_refresh";

    /** The old cookie name from the previous JWT-in-cookie scheme, purged on login/logout. */
    private static final String LEGACY_COOKIE_NAME = "JWT-TOKEN";

    // Full browser-visible path: servlet context-path (/api/v1) + the /auth mapping.
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String LEGACY_COOKIE_PATH = "/";
    private static final String SAME_SITE_LAX = "Lax";

    private final JwtProperties jwtProperties;

    /** A cookie carrying the refresh token, living as long as the refresh token itself. */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(jwtProperties.getRefreshTokenTtl()).build();
    }

    /** An immediately-expiring cookie that clears any existing refresh cookie (logout). */
    public ResponseCookie expire() {
        return base("").maxAge(0).build();
    }

    /** An immediately-expiring cookie that purges a stale legacy {@code JWT-TOKEN} cookie. */
    public ResponseCookie expireLegacy() {
        return ResponseCookie.from(LEGACY_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.getCookie().isSecure())
                .path(LEGACY_COOKIE_PATH)
                .sameSite(SAME_SITE_LAX)
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.getCookie().isSecure())
                .path(COOKIE_PATH)
                .sameSite(SAME_SITE_LAX);
    }
}
