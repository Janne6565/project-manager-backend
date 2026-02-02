package com.janne6565.projectmanager.services.external;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalContributionService {
    private final WebClient contributionApiWebClient;

    public Mono<Map<String, List<ContributionDto>>> getContributions() {
        return contributionApiWebClient.get()
                .uri("/contributions")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<ContributionDto>>>() {
                });
    }
}
