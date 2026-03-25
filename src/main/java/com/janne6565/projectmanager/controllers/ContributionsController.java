package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.dto.external.contributions.RepositoryContributionDto;
import com.janne6565.projectmanager.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contributions")
public class ContributionsController {

    private final ProjectService projectService;

    @GetMapping("/unassigned")
    public ResponseEntity<List<RepositoryContributionDto>> getUnassignedContributions() {
        return ResponseEntity.ok(projectService.getUnassignedContributions());
    }

    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Integer>> getCalendar() {
        return ResponseEntity.ok(projectService.getContributionCalendar());
    }
}
