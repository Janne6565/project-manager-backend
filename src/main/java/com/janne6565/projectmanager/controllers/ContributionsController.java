package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.dto.external.contributions.ContributionDto;
import com.janne6565.projectmanager.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contributions")
public class ContributionsController {

    private final ProjectService projectService;

    @GetMapping("/unassigned")
    public ResponseEntity<List<ContributionDto>> getUnassignedContributions() {
        return ResponseEntity.ok(projectService.getUnassignedContributions());
    }
}
