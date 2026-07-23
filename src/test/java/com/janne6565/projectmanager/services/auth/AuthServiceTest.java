package com.janne6565.projectmanager.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.entities.Role;
import com.janne6565.projectmanager.exceptions.UnauthorizedException;
import com.janne6565.projectmanager.repositories.AppUserRepository;
import com.janne6565.projectmanager.services.auth.JwtService.IssuedToken;
import com.janne6565.projectmanager.services.auth.JwtService.TokenType;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshCookieFactory refreshCookieFactory;
    @InjectMocks private AuthService authService;

    private ResponseCookie cookie(String value) {
        return ResponseCookie.from(RefreshCookieFactory.COOKIE_NAME, value).build();
    }

    private void stubSession(AppUser user, String accessToken, String refreshToken) {
        when(jwtService.issueAccessToken(user))
                .thenReturn(new IssuedToken(accessToken, Instant.now().plusSeconds(3600)));
        when(jwtService.issueRefreshToken(user))
                .thenReturn(new IssuedToken(refreshToken, Instant.now().plusSeconds(999999)));
        when(refreshCookieFactory.create(refreshToken)).thenReturn(cookie(refreshToken));
    }

    @Test
    void nullPasswordUserCannotPasswordLogin() {
        AppUser oidcUser = new AppUser("oidc", null, Role.USER);
        when(userRepository.findByUsername("oidc")).thenReturn(Optional.of(oidcUser));

        assertThatThrownBy(() -> authService.login("oidc", "anything"))
                .isInstanceOf(UnauthorizedException.class);
        // Never feed a null hash into the encoder.
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void wrongPasswordRejected() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice", "bad"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validPasswordLoginReturnsAccessTokenAndRefreshCookie() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        stubSession(user, "access-tok", "refresh-tok");

        AuthService.AuthenticatedSession session = authService.login("alice", "pw");

        assertThat(session.body().token()).isEqualTo("access-tok");
        assertThat(session.body().user().username()).isEqualTo("alice");
        assertThat(session.body().user().role()).isEqualTo(Role.ADMIN);
        assertThat(session.refreshCookie().getValue()).isEqualTo("refresh-tok");
    }

    @Test
    void disabledUserCannotPasswordLogin() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        user.setEnabled(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("alice", "pw"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshExchangesCookieForFreshAccessTokenAndRotatesCookie() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        when(jwtService.parseUserId("refresh-tok", TokenType.REFRESH)).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        stubSession(user, "new-access", "new-refresh");

        AuthService.AuthenticatedSession session = authService.refresh("refresh-tok");

        assertThat(session.body().token()).isEqualTo("new-access");
        assertThat(session.refreshCookie().getValue()).isEqualTo("new-refresh");
    }

    @Test
    void refreshRejectsMissingToken() {
        assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authService.refresh("   "))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRejectsDisabledUser() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        user.setEnabled(false);
        when(jwtService.parseUserId("refresh-tok", TokenType.REFRESH)).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh("refresh-tok"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutCookieDelegatesToFactory() {
        when(refreshCookieFactory.expire()).thenReturn(cookie(""));
        assertThat(authService.logoutCookie().getValue()).isEmpty();
    }
}
