package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.configs.oauth.FrontendProperties;
import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.exceptions.OAuthAccessDeniedException;
import com.janne6565.projectmanager.services.auth.JwtService;
import com.janne6565.projectmanager.services.auth.RefreshCookieFactory;
import com.janne6565.projectmanager.services.auth.oauth.OAuthService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Federated OIDC login ("Login with Authentik"). Both endpoints drive top-level browser navigation
 * and answer with 302 redirects — never JSON. On a successful callback it mints the app's own
 * refresh cookie for the resolved user and redirects home; every failure redirects to the login page
 * with a translated {@code oauthError} code instead of leaking a stack trace.
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private static final String ERROR_NO_ACCESS = "noAccess";
    private static final String ERROR_GENERIC = "true";

    private final OAuthService oAuthService;
    private final JwtService jwtService;
    private final RefreshCookieFactory refreshCookieFactory;
    private final FrontendProperties frontendProperties;

    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Void> authorize(@PathVariable String provider) {
        try {
            String url = oAuthService.buildAuthorizationUrl(provider);
            return redirect(url);
        } catch (RuntimeException ex) {
            log.warn("Failed to start OAuth authorize for provider {}", provider, ex);
            return redirect(loginError(ERROR_GENERIC));
        }
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        AppUser user;
        try {
            user = oAuthService.handleLoginCallback(provider, code, state);
        } catch (OAuthAccessDeniedException ex) {
            log.warn("OAuth login denied for provider {}: {}", provider, ex.getMessage());
            return redirect(loginError(ERROR_NO_ACCESS));
        } catch (RuntimeException ex) {
            log.error("OAuth callback failed for provider {}", provider, ex);
            return redirect(loginError(ERROR_GENERIC));
        }

        String refreshToken = jwtService.issueRefreshToken(user).token();
        ResponseCookie cookie = refreshCookieFactory.create(refreshToken);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .location(URI.create(frontendProperties.getUrl() + "/"))
                .build();
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private String loginError(String code) {
        return frontendProperties.getUrl() + "/login?oauthError=" + code;
    }
}
