package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.entities.Project;
import com.janne6565.projectmanager.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/projects")
public class AdminProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(@RequestParam(required = false, defaultValue = "true") boolean includeContributions) {
        List<Project> projects = projectService.getAllProjects().stream().peek(project -> {
            if (!includeContributions) project.setContributions(null);
        }).toList();

        return ResponseEntity.ok(projects);
    }
}
