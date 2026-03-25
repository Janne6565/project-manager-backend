package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.dto.external.contributions.ContributionSummaryDto;
import com.janne6565.projectmanager.services.external.ExternalContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ExternalContributionService externalContributionService;

    public ContributionSummaryDto getContributions() {
        var mono = externalContributionService.getContributions();
        return mono != null ? mono.block() : null;
    }
}
