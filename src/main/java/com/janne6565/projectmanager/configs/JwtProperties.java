package com.janne6565.projectmanager.configs;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT signing/issuing configuration, bound from {@code jwt}. The {@code secret} is a hex-encoded
 * HMAC key (kept as it was before this change); {@code accessTokenTtl}/{@code refreshTokenTtl} are
 * the two session lifetimes (env-overridable via {@code JWT_ACCESS_TOKEN_TTL}/{@code
 * JWT_REFRESH_TOKEN_TTL}). {@code cookie.secure} toggles the {@code Secure} attribute on the refresh
 * cookie (true everywhere real; false only for plain-HTTP local dev).
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Hex-encoded HMAC-SHA signing key. */
    private String secret;

    private Duration accessTokenTtl = Duration.ofHours(1);

    private Duration refreshTokenTtl = Duration.ofDays(30);

    private String issuer = "project-manager";

    private final Cookie cookie = new Cookie();

    @Data
    public static class Cookie {
        private boolean secure = true;
    }
}
