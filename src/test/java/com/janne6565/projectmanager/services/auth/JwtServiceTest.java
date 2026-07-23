package com.janne6565.projectmanager.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.janne6565.projectmanager.configs.JwtProperties;
import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.entities.Role;
import com.janne6565.projectmanager.exceptions.UnauthorizedException;
import com.janne6565.projectmanager.services.auth.JwtService.TokenType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure-JVM tests for token issuing/verification (no Spring context, no Docker). */
class JwtServiceTest {

    // 32-byte hex-encoded HMAC key, same encoding the app reads from jwt.secret.
    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static JwtProperties properties(String secret, Duration accessTtl, Duration refreshTtl) {
        JwtProperties p = new JwtProperties();
        p.setSecret(secret);
        p.setAccessTokenTtl(accessTtl);
        p.setRefreshTokenTtl(refreshTtl);
        p.setIssuer("project-manager");
        return p;
    }

    private final JwtService jwtService =
            new JwtService(properties(SECRET, Duration.ofHours(1), Duration.ofDays(30)));

    @Test
    void issuesAccessTokenThatParsesBackToTheSameUserId() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);

        JwtService.IssuedToken issued = jwtService.issueAccessToken(user);

        assertThat(issued.expiresAt()).isAfter(Instant.now());
        assertThat(jwtService.parseUserId(issued.token(), TokenType.ACCESS)).isEqualTo(user.getId());
    }

    @Test
    void issuesRefreshTokenThatParsesBackToTheSameUserId() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);

        JwtService.IssuedToken issued = jwtService.issueRefreshToken(user);

        assertThat(jwtService.parseUserId(issued.token(), TokenType.REFRESH))
                .isEqualTo(user.getId());
    }

    @Test
    void rejectsRefreshTokenWhenAccessExpected() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        String refresh = jwtService.issueRefreshToken(user).token();

        assertThatThrownBy(() -> jwtService.parseUserId(refresh, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAccessTokenWhenRefreshExpected() {
        AppUser user = new AppUser("alice", "hash", Role.ADMIN);
        String access = jwtService.issueAccessToken(user).token();

        assertThatThrownBy(() -> jwtService.parseUserId(access, TokenType.REFRESH))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsGarbageToken() {
        assertThatThrownBy(() -> jwtService.parseUserId("not-a-jwt", TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        JwtService other =
                new JwtService(
                        properties(
                                "0011223344556677889900AABBCCDDEEFF00112233445566778899AABBCCDDEE",
                                Duration.ofHours(1),
                                Duration.ofDays(30)));
        String foreignToken =
                other.issueAccessToken(new AppUser("mallory", "hash", Role.USER)).token();

        assertThatThrownBy(() -> jwtService.parseUserId(foreignToken, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived =
                new JwtService(properties(SECRET, Duration.ofSeconds(-1), Duration.ofDays(30)));
        String expired = shortLived.issueAccessToken(new AppUser("bob", "hash", Role.USER)).token();

        assertThatThrownBy(() -> jwtService.parseUserId(expired, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }
}
