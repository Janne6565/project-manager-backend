package com.janne6565.projectmanager.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${external.contributions.url}")
    private String contributionApiUrl;

    @Bean
    public WebClient contributionApiWebClient() {
        return WebClient.create(contributionApiUrl);
    }
}
