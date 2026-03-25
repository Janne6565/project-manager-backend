package com.janne6565.projectmanager.dto.external.contributions;

public record ContributionTotalsDto(
        int commits,
        int pullRequests,
        int issues,
        int reviews
) {
}
