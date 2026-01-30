package com.janne6565.projectmanager.security;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.security.external.ExternalContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ExternalContributionService externalContributionService;

    public List<ContributionDto> getContributions() {
        return List.of();
    }

    public Project updateProjectContributions(Project project, List<ContributionDto> contributions) {
        Project projectCopy = project.copy();
        Map<String, List<ContributionDto>> existingContributionsByDay = new HashMap<>();
        for (ContributionDto contribution : project.getContributions()) {
            existingContributionsByDay.putIfAbsent(contribution.day(), new ArrayList<>());
            existingContributionsByDay.get(contribution.day()).add(contribution);
        }

        Map<String, List<ContributionDto>> externalContributionsMap = new HashMap<>();
        for (ContributionDto contribution : contributions) {
            externalContributionsMap.putIfAbsent(contribution.day(), new ArrayList<>());
            externalContributionsMap.get(contribution.day()).add(contribution);
        }

        for (String day : externalContributionsMap.keySet()) {
            projectCopy.getContributions().addAll(externalContributionsMap.get(day));
        }

        return projectCopy;
    }
}
