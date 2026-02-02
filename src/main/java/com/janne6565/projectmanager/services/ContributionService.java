package com.janne6565.projectmanager.services;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.services.external.ExternalContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ExternalContributionService externalContributionService;

    public List<ContributionDto> getContributions() {
        Map<String, List<ContributionDto>> contributionsMap = externalContributionService.getContributions().block();
        return contributionsMap.values().stream().flatMap(List::stream).toList();
    }

    public Project updateProjectContributions(Project project, List<ContributionDto> contributions) {
        Project projectCopy = project.copy();
        ArrayList<ContributionDto> contributionsList = new ArrayList<>();

        Set<String> seenDays = new HashSet<>();
        for (ContributionDto contribution : contributions) {
            seenDays.add(contribution.day());
            contributionsList.add(contribution);
        }

        Map<String, List<ContributionDto>> existingContributionsByDay = new HashMap<>();
        if (project.getContributions() != null) {
            for (ContributionDto contribution : project.getContributions()) {
                if (seenDays.contains(contribution.day())) {
                    continue;
                }
                existingContributionsByDay.putIfAbsent(contribution.day(), new ArrayList<>());
                existingContributionsByDay.get(contribution.day()).add(contribution);
            }
        }

        projectCopy.setContributions(contributionsList);

        return projectCopy;
    }
}
