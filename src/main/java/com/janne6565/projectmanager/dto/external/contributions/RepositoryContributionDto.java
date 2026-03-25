package com.janne6565.projectmanager.dto.external.contributions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryContributionDto(
        String url,
        String name,
        int commits,
        int pullRequests,
        int issues,
        int reviews
) {
}
