package com.janne6565.projectmanager.dto.external.contributions;

import java.util.List;
import java.util.Map;

public record ContributionSummaryDto(
        Map<String, Integer> calendar,
        List<RepositoryContributionDto> repositories,
        ContributionTotalsDto totals
) {
}
