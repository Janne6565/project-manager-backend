package com.janne6565.projectmanager;

import com.janne6565.projectmanager.dto.external.contributions.ContributionSummaryDto;
import com.janne6565.projectmanager.dto.external.contributions.ContributionTotalsDto;
import com.janne6565.projectmanager.services.external.ExternalContributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

@SpringBootTest
@ActiveProfiles("test")
class ProjectManagerApplicationTests {

    @MockitoBean
    private ExternalContributionService externalContributionService;

    @BeforeEach
    void setup() {
        ContributionSummaryDto emptySummary = new ContributionSummaryDto(
                java.util.Map.of(), java.util.List.of(), new ContributionTotalsDto(0, 0, 0, 0));
        org.mockito.Mockito.when(externalContributionService.getContributions())
                .thenReturn(Mono.just(emptySummary));
    }

    @Test
    void contextLoads() {
    }

}
