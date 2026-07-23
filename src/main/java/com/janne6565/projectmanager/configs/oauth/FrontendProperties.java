package com.janne6565.projectmanager.configs.oauth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Frontend location, bound from {@code projectmanager.frontend}. Used to build the browser redirect
 * targets after an OAuth callback (success and error).
 */
@Data
@Component
@ConfigurationProperties(prefix = "projectmanager.frontend")
public class FrontendProperties {
    private String url;
}
