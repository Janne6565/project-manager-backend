package com.janne6565.projectmanager.configs.oauth;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Federated OIDC login configuration. Bound from {@code projectmanager.oauth}. Each entry in {@code
 * providers} is an authorization-code provider (the pilot ships a single {@code authentik} entry);
 * {@code groups} names the Authentik groups that gate access and map to the coarse role.
 *
 * <p>{@code clientId}/{@code clientSecret} may be blank so the app still boots in local dev without
 * credentials — the OAuth endpoints simply won't authenticate until they are supplied via
 * environment.
 */
@Data
@Component
@ConfigurationProperties(prefix = "projectmanager.oauth")
public class OAuthProperties {

    private Map<String, Provider> providers = new HashMap<>();

    private Groups groups = new Groups();

    @Data
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String scope;
        private String callbackUri;
    }

    @Data
    public static class Groups {
        private String admin;
        private String user;
    }
}
