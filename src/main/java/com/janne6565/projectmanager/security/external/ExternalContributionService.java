package com.janne6565.projectmanager.security.external;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalContributionService {
    private final WebClient contributionApiWebClient;

    public Mono<Map<String, ContributionDto>> getContributions(String username) {
        return contributionApiWebClient.get()
                .uri("/contributions")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, ContributionDto>>() {
                });
    }
}
