package com.janne6565.projectmanager.services.external;

import com.janne6565.projectmanager.dto.external.contributions.ContributionSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ExternalContributionService {
    private final WebClient contributionApiWebClient;

    public Mono<ContributionSummaryDto> getContributions() {
        return contributionApiWebClient.get()
                .uri("/contributions")
                .retrieve()
                .bodyToMono(ContributionSummaryDto.class);
    }
}
